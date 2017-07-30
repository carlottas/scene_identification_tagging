package it.emarolab.scene_identification_tagging.Score;

import it.emarolab.owloop.aMORDescriptor.utility.concept.MORFullConcept;
//import jdk.internal.org.objectweb.asm.tree.analysis.Value;

import it.emarolab.owloop.core.Semantic;
import org.apache.jena.base.Sys;
import sit_msgs.*;
import org.ros.node.ConnectedNode;
import org.ros.node.service.ServiceResponseBuilder;
import org.ros.internal.message.Message;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.Node;
import org.ros.node.parameter.ParameterTree;


import it.emarolab.amor.owlInterface.OWLReferences;
import it.emarolab.amor.owlInterface.OWLReferencesInterface;
import it.emarolab.owloop.aMORDescriptor.MORAxioms;
import it.emarolab.owloop.aMORDescriptor.utility.individual.MORFullIndividual;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import it.emarolab.scene_identification_tagging.Interfaces.*;

import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
public interface ScoreJAVAInterface
{
    /**
     * Class which manage the semantic score initialization, update and removal;
     */
    class SemanticScore implements SITBase,MemoryInterface {
        private String Name;
        private List<String> firstSuperClass;
        private List<String> subClasses = new ArrayList<>();
        private List<String> superClasses = new ArrayList<>();
        private List<String> belongingIndividuals = new ArrayList<>();
        private List<String> isFirstSuperCLassOf;
        MORFullIndividual scoreSemantic;
        MORFullIndividual totalScoreSemantic;
        MORFullIndividual totalScoreEpisodic;
        boolean addTime = true;
        private Long time = System.currentTimeMillis();
        OWLReferences ontoRef;

        /**
         * Constructor
         *
         * @param Name            Name of the semanticScore Individual
         * @param SubClasses      List of Name of the subClasses of the semanticScore Individual
         * @param SuperClasses    List of the Name of the superClasses of the semantic Score Individual
         * @param firstSuperClass String containing the Name of the first super class of the semantic score individal
         * @param ontoRef         Reference to the ontology to be manipulated
         * @param AddTime         Boolean, if true also the time information are added
         */
        public SemanticScore(String Name, List<String> SubClasses,
                             List<String> SuperClasses, List<String> firstSuperClass, List<String> isFirstSuperCLassOf, OWLReferences ontoRef, boolean AddTime) {

            scoreSemantic = new MORFullIndividual(Name,
                    ontoRef);
            totalScoreSemantic = new MORFullIndividual(SCORE.SCORE_INDIVIDUAL_TOTAL_SEMANTIC,
                    ontoRef);
            totalScoreEpisodic = new MORFullIndividual(SCORE.SCORE_INDIVIDUAL_TOTAL_EPISODIC,
                    ontoRef);
            MORFullIndividual clock = new MORFullIndividual(TIME.CLOCK,ontoRef);
            clock.readSemantic();
            clock.removeData(SCORE.SCORE_PROP_HAS_TIME);
            clock.addData(SCORE.SCORE_PROP_HAS_TIME,System.currentTimeMillis());
            clock.writeSemantic();

            this.ontoRef = ontoRef;
            this.Name = Name;
            SubClasses.remove("owlNothing");
            this.subClasses = SubClasses;
            SuperClasses.remove(CLASS.SCENE);
            this.superClasses = SuperClasses;
            this.firstSuperClass = firstSuperClass;
            this.isFirstSuperCLassOf = isFirstSuperCLassOf;
            this.addTime = AddTime;

        }

        /**
         * Constructor when the item already exists in the ontology
         *
         * @param Name    Name of the semantic Score individual
         * @param ontoRef Reference to the Ontology to be manipulated
         */
        public SemanticScore(String Name, OWLReferences ontoRef) {
            scoreSemantic = new MORFullIndividual(Name,
                    ontoRef);
            totalScoreSemantic = new MORFullIndividual(SCORE.SCORE_INDIVIDUAL_TOTAL_SEMANTIC,
                    ontoRef);
            totalScoreEpisodic = new MORFullIndividual(SCORE.SCORE_INDIVIDUAL_TOTAL_EPISODIC,
                    ontoRef);

            MORFullIndividual clock = new MORFullIndividual(TIME.CLOCK,ontoRef);
            clock.readSemantic();
            clock.removeData(SCORE.SCORE_PROP_HAS_TIME);
            clock.addData(SCORE.SCORE_PROP_HAS_TIME,System.currentTimeMillis());
            clock.writeSemantic();

            this.ontoRef = ontoRef;
            this.Name = Name;
            ontoRef.synchronizeReasoner();
            scoreSemantic.readSemantic();
            totalScoreSemantic.readSemantic();
            totalScoreEpisodic.readSemantic();
            //initializing the other variables
            updateAllRelations();

        }

        /**
         * function which creates the semantic score item in the ontology, compute its score and link it to the subClasses,
         * SuperClasses and Belonging Individuals, Also the superclasses' scores are updated
         */
        public void semanticInitialization() {
            // add the individual to the class
            System.out.println("creating the semantic score individuals "+Name);
            ontoRef.synchronizeReasoner();
            scoreSemantic.readSemantic();
            scoreSemantic.addTypeIndividual(SCORE.SCORE_CLASS_SEMANTIC_SCORE);
            scoreSemantic.writeSemantic();
            ontoRef.synchronizeReasoner();
            scoreSemantic.readSemantic();
            // add the corresponding data properties
            System.out.println("adding the data properties..");
            scoreSemantic.addData(SCORE.SCORE_PROP_NUMBER_BELONGING_INDIVIDUAL, 0,true);
            scoreSemantic.addData(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL, 0.0,true);
            scoreSemantic.addData(SCORE.SCORE_PROP_NUMBER_RETRIEVAL, 1,true);
            scoreSemantic.addData(SCORE.SCORE_PROP_NUMBER_SUB_CLASSES, subClasses.size());
            scoreSemantic.addData(SCORE.SCORE_PROP_SCORE_SUM_SUB_CLASSES, computeSubClassesScore());
            scoreSemantic.addData(SCORE.SCORE_PROP_TIMES_FORGOTTEN, 0,true);
            scoreSemantic.addData(SCORE.SCORE_PROP_TIMES_LOW_SCORE, 0,true);
            scoreSemantic.addData(SCORE.SCORE_PROP_USER_NO_FORGET, false, true);
            if (addTime) {
                scoreSemantic.addData(SCORE.SCORE_PROP_HAS_TIME, time);
            }
            scoreSemantic.writeSemantic();
            ontoRef.synchronizeReasoner();
            scoreSemantic.saveOntology(SCORE.SCORE_FILE_PATH);
            scoreSemantic.readSemantic();
            //compute the score
            System.out.println("Computing and adding the score...");
            float scoreComputed = computeScore(scoreSemantic);
            //add the score to the individual
            scoreSemantic.addData(SCORE.SCORE_PROP_HAS_SCORE, scoreComputed,true);
            scoreSemantic.writeSemantic();
            //Updating total score
            System.out.println("Updating total score..");
            UpdateTotalSemanticScore(scoreComputed);
            System.out.println("adding object properties...");
            //adding the property is superClassOf
            for (String s : subClasses) {
                scoreSemantic.addObject(SCORE.SCORE_OBJ_PROP_IS_SUPER_CLASS_OF, s);
            }
            //adding data prop is subclass of
            for (String s : superClasses) {
                scoreSemantic.addObject(SCORE.SCORE_OBJ_PROP_IS_SUB_CLASS_OF, s);
            }

            //adding obj property first super Class
            // for (String s : this.firstSuperClass) {
            //    scoreSemantic.addObject(SCORE.SCORE_OBJ_PROP_FIRST_SUPERCLASS, s);
            //}
            //for (String s : this.isFirstSuperCLassOf) {
            //   MORFullIndividual ind = new MORFullIndividual(s, ontoRef);
            //   ind.readSemantic();
            //   ind.removeObject(SCORE.SCORE_OBJ_PROP_FIRST_SUPERCLASS);
            //   ind.addObject(SCORE.SCORE_OBJ_PROP_FIRST_SUPERCLASS, this.Name);
            //   ind.writeSemantic();

            // }
            scoreSemantic.writeSemantic();
            ontoRef.synchronizeReasoner();
            scoreSemantic.saveOntology(SCORE.SCORE_FILE_PATH);
            System.out.println("created item \n \n "+scoreSemantic);
            //updating super class score
            System.out.println("updating super classes score...");
            updateSuperClassScore(superClasses, scoreComputed);
            float delete= (float)0.0;
           // ontoRef.removeDataPropertyB2Individual(Name,SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL,delete);
           // ontoRef.saveOntology(SCORE.SCORE_FILE_PATH);
            //updateSemanticFromIndividual("sn",(float)2.0);
        }

        /**
         * Function which update the semantic score if it is retrieved
         */
        public void semanticRetrieval() {
            scoreSemantic.readSemantic();
            //updating The score of the belonging individuals
            System.out.println("updating score of the belonging individual for the semantic item "+Name);
            for (String s : belongingIndividuals) {
                EpisodicScore ep = new EpisodicScore(s, ontoRef);
                ArrayList<Float> values=ep.episodicSemanticRetrieval();
                updateSemanticFromIndividual(values.get(0),values.get(1));
            }
            scoreSemantic.readSemantic();
            //updating the retrieval number
            System.out.println("updating the number of retrieval for the item "+Name);
            int retrieval = scoreSemantic.getLiteral(SCORE.SCORE_PROP_NUMBER_RETRIEVAL).parseInteger();
            retrieval++;
            //updating the dataproperty with the new value
            scoreSemantic.removeData(SCORE.SCORE_PROP_NUMBER_RETRIEVAL);
            scoreSemantic.writeSemantic();
            ontoRef.synchronizeReasoner();
            scoreSemantic.readSemantic();
            scoreSemantic.addData(SCORE.SCORE_PROP_NUMBER_RETRIEVAL, retrieval,true);
            scoreSemantic.writeSemantic();
            ontoRef.synchronizeReasoner();
            scoreSemantic.readSemantic();
            //updating the score
            System.out.println("updating the score of "+Name);
            float oldScore= scoreSemantic.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat();
            float newScore = computeScore(scoreSemantic);
            //updating the total score Semantic
            System.out.println("updating the total semantic score... ");
            UpdateTotalSemanticScore(oldScore, newScore);
            //update the superClasses List
            SuperClasses();
            //update superClasses Score
            System.out.println("update superclasses score...");
            updateSuperClassScore(superClasses,oldScore,newScore);
            //updating the score value;
            scoreSemantic.removeData(SCORE.SCORE_PROP_HAS_SCORE);
            scoreSemantic.writeSemantic();
            ontoRef.synchronizeReasoner();
            scoreSemantic.readSemantic();
            scoreSemantic.addData(SCORE.SCORE_PROP_HAS_SCORE, newScore,true);
            //saving the ontology
            scoreSemantic.writeSemantic();
            ontoRef.synchronizeReasoner();
            scoreSemantic.saveOntology(SCORE.SCORE_FILE_PATH);
        }
        /**
         * @return sum of the score of the subclasses
         */
        private float computeSubClassesScore() {
            //if the set is empty hence there is no subclass return 0
            if (this.subClasses.isEmpty()) {
                return 0;
            }
            float total = 0;
            //for all the subclasses
            for (String nameSubClass : subClasses) {
                MORFullIndividual ind = new MORFullIndividual(nameSubClass, ontoRef);
                //read the current state of the individual
                ind.readSemantic();
                //adding to the total the value of dataproperty hasScore
                total += ind.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat();
            }
            //return the total just computed
            return total;
        }

        /**
         * @param ind individual of which the score must be computed
         * @return the score
         */
        private float computeScore(MORFullIndividual ind) {
            //read the current state of the ontology
            ontoRef.synchronizeReasoner();
            ind.readSemantic();
            totalScoreSemantic.readSemantic();
            float scoreSubClasses;
            float scoreIndividual;
            int retrieval = ind.getLiteral(SCORE.SCORE_PROP_NUMBER_RETRIEVAL).parseInteger();
            int numberBelongingIndividual = ind.getLiteral(SCORE.SCORE_PROP_NUMBER_BELONGING_INDIVIDUAL).parseInteger();
            int numberSubClasses = ind.getLiteral(SCORE.SCORE_PROP_NUMBER_SUB_CLASSES).parseInteger();
            //if the number of SubClasses is equal to 0 then also the score of subclasses is equal to 0
            if (numberSubClasses == 0) {
                scoreSubClasses = 0;
            } else {
                scoreSubClasses = ind.getLiteral(SCORE.SCORE_PROP_SCORE_SUM_SUB_CLASSES).parseFloat() / numberSubClasses;
            }
            // if there the number of individuals is equal to 0 then also the score of belonging individuals is equal
            //to 0
            if (numberBelongingIndividual == 0) {
                scoreIndividual = 0;
            } else {
                scoreIndividual = ind.getLiteral(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL).parseFloat() / numberBelongingIndividual;
            }

            return ((float) (SCORE.SCORE_SEMANTIC_WEIGHT_1 * numberBelongingIndividual +
                    SCORE.SCORE_SEMANTIC_WEIGHT_2 * scoreIndividual +
                    SCORE.SCORE_SEMANTIC_WEIGHT_3 * numberSubClasses +
                    SCORE.SCORE_SEMANTIC_WEIGHT_4 * scoreSubClasses +
                    SCORE.SCORE_SEMANTIC_WEIGHT_5 * retrieval));
        }

        /**
         * Compute the score
         * @param numberBelongingIndividual
         * @param numberSubClasses
         * @param sumScoreBelongingIndividua
         * @param sumScoreSubClasses
         * @param retrieval
         * @return
         */
        private float computeScore(int numberBelongingIndividual, int numberSubClasses, float sumScoreBelongingIndividua,
                                   float sumScoreSubClasses, int retrieval) {
            float scoreSubClasses;
            float scoreIndividual;

            if (numberSubClasses == 0) {
                scoreSubClasses = 0;
            } else {
                scoreSubClasses = sumScoreSubClasses / numberSubClasses;
            }
            // if the total episodic is equal to 0
            if (numberBelongingIndividual == 0) {
                scoreIndividual = 0;
            } else {
                scoreIndividual = sumScoreBelongingIndividua / numberBelongingIndividual;
            }
            return ((float) (SCORE.SCORE_SEMANTIC_WEIGHT_1 * numberBelongingIndividual +
                    SCORE.SCORE_SEMANTIC_WEIGHT_2 * scoreIndividual +
                    SCORE.SCORE_SEMANTIC_WEIGHT_3 * numberSubClasses +
                    SCORE.SCORE_SEMANTIC_WEIGHT_4 * scoreSubClasses +
                    SCORE.SCORE_SEMANTIC_WEIGHT_5 * retrieval));
        }

        /**
         * update the total semantic score  when a new item is added
         *
         * @param scoreComputed :score of the semantic item added
         */
        public void UpdateTotalSemanticScore(float scoreComputed) {
            //read the current state of the total semnatic score
            ontoRef.synchronizeReasoner();
            totalScoreSemantic.readSemantic();
            //reading the data property "has value"
            float Total = totalScoreSemantic.getLiteral(SCORE.SCORE_PROP_HAS_VALUE).parseFloat();
            if (Total == 0.0) {
                Total = scoreComputed;
            } else {
                //change the value by adding the new score
                Total += scoreComputed;
            }
            //change the dataproperty value
            totalScoreSemantic.removeData(SCORE.SCORE_PROP_HAS_VALUE);
            totalScoreSemantic.addData(SCORE.SCORE_PROP_HAS_VALUE, Total,true);
            totalScoreSemantic.writeSemantic();

        }

        /**
         * Update the total Semantic Score when the score of an Item is changed
         *
         * @param oldScore of the semantic item modified
         * @param newScore of the semantic item modified
         */
        public void UpdateTotalSemanticScore(float oldScore, float newScore) {
            //read the current state of total semantic item
            ontoRef.synchronizeReasoner();
            totalScoreSemantic.readSemantic();
            //reading the value of hasValue dataproperty
            float total = totalScoreSemantic.getLiteral(SCORE.SCORE_PROP_HAS_VALUE).parseFloat();
            total -= oldScore;
            total += newScore;
            //updating the data property with the new value just computed
            totalScoreSemantic.removeData(SCORE.SCORE_PROP_HAS_VALUE);
            totalScoreSemantic.writeSemantic();
            ontoRef.synchronizeReasoner();
            totalScoreSemantic.readSemantic();
            totalScoreSemantic.addData(SCORE.SCORE_PROP_HAS_VALUE, total,true);
            totalScoreSemantic.writeSemantic();
            ontoRef.synchronizeReasoner();
            totalScoreSemantic.saveOntology(SCORE.SCORE_FILE_PATH);
        }

        /**
         * Updating superclasses score when a score of a subclass has changed
         *
         * @param classesNames names of the superclasses to be updated
         * @param scoreOld     old score of the changed semantic item
         * @param scoreNew     new score of the changed semantic item
         */
        public void updateSuperClassScore(List<String> classesNames, float scoreOld, float scoreNew) {
            //if the set of string is empty hence there is no super class the functions
            //automatically returns
            if (classesNames.isEmpty()) {
                return;
            }
            //for all the superClasses
            for (String name : classesNames) {
                //additional check in order not to analyze the wrong classes
                if (!name.equals(CLASS.SCENE)) {
                    //define the MOR individual of such superclass
                    MORFullIndividual superClass = new MORFullIndividual(
                            name,
                            ontoRef
                    );
                    //read the ontology
                    ontoRef.synchronizeReasoner();
                    superClass.readSemantic();
                    //store the old score
                    float oldScore = superClass.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat();
                    //update the subclasses score with the new one
                    float scoreSubClasses = superClass.getLiteral(SCORE.SCORE_PROP_SCORE_SUM_SUB_CLASSES).parseFloat();
                    scoreSubClasses -= scoreOld;
                    scoreSubClasses += scoreNew;
                    superClass.removeData(SCORE.SCORE_PROP_SCORE_SUM_SUB_CLASSES);
                    superClass.writeSemantic();
                    ontoRef.synchronizeReasoner();
                    superClass.readSemantic();
                    superClass.addData(SCORE.SCORE_PROP_SCORE_SUM_SUB_CLASSES, scoreSubClasses,true);
                    superClass.writeSemantic();
                    //compute the new score
                    float newScore = computeScore(superClass);
                    //change the value of the data prop score
                    superClass.removeData(SCORE.SCORE_PROP_HAS_SCORE);
                    superClass.writeSemantic();
                    ontoRef.synchronizeReasoner();
                    superClass.readSemantic();
                    superClass.addData(SCORE.SCORE_PROP_HAS_SCORE, newScore,true);
                    //write the semantic
                    superClass.writeSemantic();
                    ontoRef.synchronizeReasoner();
                    superClass.readSemantic();
                    //update total semantic score
                    UpdateTotalSemanticScore(oldScore, newScore);
                    //find the super classes of such element
                    MORAxioms.ObjectSemantics objProp = superClass.getObjectSemantics();
                    //check if there is any superclasses
                    List<String> classes = new ArrayList<>();
                    memory.objectPropertyValues(objProp, SCORE.SCORE_OBJ_PROP_IS_SUB_CLASS_OF, classes,SCORE.SCORE_IRI_ONTO);
                    ontoRef.synchronizeReasoner();
                    superClass.saveOntology(SCORE.SCORE_FILE_PATH);
                    //update superclasses score
                    updateSuperClassScore(classes, oldScore, newScore);
                }
            }
        }

        /**
         * Update superclasses score when a new subclass has been added
         *
         * @param setName name of the superclasses to be updated
         * @param score   score of the semantic item to be added
         */
        public void updateSuperClassScore(List<String> setName, float score) {
            //if the set of string is empty hence there is no super class the functions
            //automatically returns
            if (setName.isEmpty()) {
                return;
            }
            //for all the superClasses
            for (String name : setName) {
                //define the MOR individual of such superclass
                MORFullIndividual superClass = new MORFullIndividual(
                        name,
                        ontoRef
                );
                //read the ontology
                ontoRef.synchronizeReasoner();
                superClass.readSemantic();
                //store the old score
                float oldScore = superClass.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat();
                //update the subclasses score with the new one
                float scoreSubClasses = superClass.getLiteral(SCORE.SCORE_PROP_SCORE_SUM_SUB_CLASSES).parseFloat();
                scoreSubClasses += score;
                superClass.removeData(SCORE.SCORE_PROP_SCORE_SUM_SUB_CLASSES);
                superClass.writeSemantic();
                ontoRef.synchronizeReasoner();
                superClass.readSemantic();
                superClass.addData(SCORE.SCORE_PROP_SCORE_SUM_SUB_CLASSES, scoreSubClasses,true);
                //update the number of subclasses
                int numberSubClasses = superClass.getLiteral(SCORE.SCORE_PROP_NUMBER_SUB_CLASSES).parseInteger();
                numberSubClasses++;
                superClass.removeData(SCORE.SCORE_PROP_NUMBER_SUB_CLASSES);
                superClass.writeSemantic();
                ontoRef.synchronizeReasoner();
                superClass.readSemantic();
                superClass.addData(SCORE.SCORE_PROP_NUMBER_SUB_CLASSES, numberSubClasses,true);
                superClass.writeSemantic();
                //compute the new score
                float newScore = computeScore(superClass);
                //change the value of the data prop score
                superClass.removeData(SCORE.SCORE_PROP_HAS_SCORE);
                superClass.writeSemantic();
                ontoRef.synchronizeReasoner();
                superClass.readSemantic();
                superClass.addData(SCORE.SCORE_PROP_HAS_SCORE, newScore);
                //write the semantic
                superClass.writeSemantic();
                ontoRef.synchronizeReasoner();
                superClass.readSemantic();
                //update total semantic score
                UpdateTotalSemanticScore(oldScore, newScore);
                //find the super classes of the superClass
                MORAxioms.ObjectSemantics objProp = superClass.getObjectSemantics();
                //check if there is any subclasses
                List<String> classes = new ArrayList<>();
                memory.objectPropertyValues(objProp, SCORE.SCORE_OBJ_PROP_IS_SUB_CLASS_OF, classes,SCORE.SCORE_IRI_ONTO);
                //update superclasses score
                updateSuperClassScore(classes, oldScore, newScore);
            }
        }

        /**
         * FUnction which delete  the influence of the score item from its superClasses
         */
        public void deleteFromSuperClasses() {
            for (String sup : this.superClasses) {
                //declare the object
                MORFullIndividual Sup = new MORFullIndividual(sup, ontoRef);
                ontoRef.synchronizeReasoner();
                //read the current ontology state
                Sup.readSemantic();
                scoreSemantic.readSemantic();
                //storing the old score value of the superClass
                float oldScore= Sup.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat();
                //remove the objectProperty is superclass of
                Sup.removeObject(SCORE.SCORE_OBJ_PROP_IS_SUPER_CLASS_OF, this.Name);
                scoreSemantic.removeObject(SCORE.SCORE_OBJ_PROP_IS_SUB_CLASS_OF,sup);
                //update the number of subclass
                int numberSubClasses = Sup.getLiteral(SCORE.SCORE_PROP_NUMBER_SUB_CLASSES).parseInteger();
                numberSubClasses--;
                //update the score of subclass
                float scoreSubClasses = Sup.getLiteral(SCORE.SCORE_PROP_SCORE_SUM_SUB_CLASSES).parseFloat();
                scoreSubClasses -= scoreSemantic.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat();
                Sup.removeData(SCORE.SCORE_PROP_NUMBER_SUB_CLASSES);
                Sup.writeSemantic();
                ontoRef.synchronizeReasoner();
                Sup.readSemantic();
                Sup.addData(SCORE.SCORE_PROP_NUMBER_SUB_CLASSES, numberSubClasses,true);
                Sup.removeData(SCORE.SCORE_PROP_SCORE_SUM_SUB_CLASSES);
                Sup.writeSemantic();
                ontoRef.synchronizeReasoner();
                Sup.readSemantic();
                Sup.addData(SCORE.SCORE_PROP_SCORE_SUM_SUB_CLASSES, scoreSubClasses,true);
                //updating the ontology
                Sup.writeSemantic();
                scoreSemantic.writeSemantic();
                //compute the new score
                float newScore = computeScore(Sup);
                //update the semantic with the new score
                UpdateTotalSemanticScore(oldScore, newScore);
                //update the ontology
                Sup.removeData(SCORE.SCORE_PROP_HAS_SCORE);
                Sup.writeSemantic();
                ontoRef.synchronizeReasoner();
                Sup.readSemantic();
                Sup.addData(SCORE.SCORE_PROP_HAS_SCORE, newScore,true);
                Sup.writeSemantic();
                //update super classses score
                //todo CHeck if it works
                List<String> supClasses= new ArrayList<>();
                memory.objectPropertyValues(Sup.getObjectSemantics(),SCORE.SCORE_OBJ_PROP_IS_SUB_CLASS_OF,supClasses,SCORE.SCORE_IRI_ONTO);
                updateSuperClassScore(supClasses,oldScore,newScore );

            }

        }

        /**
         * Function which deletes the information about an episodic item form the semantic item
         *
         * @param s     name of the episodic item
         * @param score value of its score
         */
        public void deleteEpisodicItem(String s, float score) {
            //Syncronize reasoner
            ontoRef.synchronizeReasoner();
            //read the current state of the ontology
            scoreSemantic.readSemantic();
            //storing the old score value
            float oldScore = scoreSemantic.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat();
            //removing the object property hasIndividual
            scoreSemantic.removeObject(SCORE.SCORE_OBJ_PROP_HAS_INDIVIDUAL, s);
            //update the number of belonging individual
            int numberOfBelongingIndividual = scoreSemantic.getLiteral(SCORE.SCORE_PROP_NUMBER_BELONGING_INDIVIDUAL).parseInteger() - 1;
            //update the score of belonging individual
            float scoreOfBelongingIndividual = scoreSemantic.getLiteral(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL).parseFloat() - score;
            scoreSemantic.removeData(SCORE.SCORE_PROP_NUMBER_BELONGING_INDIVIDUAL);
            scoreSemantic.writeSemantic();
            ontoRef.synchronizeReasoner();
            scoreSemantic.readSemantic();
            scoreSemantic.addData(SCORE.SCORE_PROP_NUMBER_BELONGING_INDIVIDUAL, numberOfBelongingIndividual,true);
            scoreSemantic.writeSemantic();
            ontoRef.synchronizeReasoner();
            scoreSemantic.readSemantic();
            scoreSemantic.removeData(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL);
            scoreSemantic.writeSemantic();
            ontoRef.synchronizeReasoner();
            scoreSemantic.readSemantic();
            scoreSemantic.addData(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL, scoreOfBelongingIndividual,true);
            //update the ontology
            scoreSemantic.writeSemantic();
            ontoRef.synchronizeReasoner();
            scoreSemantic.saveOntology(SCORE.SCORE_FILE_PATH);
            //compute the new score
            float newScore = computeScore(scoreSemantic);
            //update the ontology
            scoreSemantic.removeData(SCORE.SCORE_PROP_HAS_SCORE);
            scoreSemantic.writeSemantic();
            ontoRef.synchronizeReasoner();
            scoreSemantic.readSemantic();
            scoreSemantic.addData(SCORE.SCORE_PROP_HAS_SCORE, newScore,true);
            scoreSemantic.writeSemantic();
            ontoRef.synchronizeReasoner();
            scoreSemantic.saveOntology(SCORE.SCORE_FILE_PATH);
            //update total semantic score
            UpdateTotalSemanticScore(oldScore, newScore);
            //update super classes score
            updateSuperClassScore(superClasses, oldScore, newScore);
        }

        /**
         * Function which updates the score of the semantic item to which
         * a new episodic score belong
         *
         * @param episodicName name of the episodic item
         * @param Score        score of the item
         */
        public void updateSemanticFromIndividual(String episodicName, float Score) {
            //reading the ontology
            //ontoRef.synchronizeReasoner();

            scoreSemantic.readSemantic();
            //storing the old score
            float oldScoreSemantic= scoreSemantic.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat();
            //updating score Belonging individuals
            float scoreBelongingIndividual = scoreSemantic.getLiteral(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL).parseFloat();
            float oldValue = scoreBelongingIndividual;
            int numberBelongingIndividual = scoreSemantic.getLiteral(SCORE.SCORE_PROP_NUMBER_BELONGING_INDIVIDUAL).parseInteger();
            int oldNumber= numberBelongingIndividual;
            numberBelongingIndividual++;
            scoreBelongingIndividual += Score;
            ontoRef.synchronizeReasoner();
            ontoRef.removeDataPropertyB2Individual(Name,SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL,(float)oldValue);
            ontoRef.saveOntology(SCORE.SCORE_FILE_PATH);
            ontoRef.synchronizeReasoner();
            ontoRef.removeDataPropertyB2Individual(Name,SCORE.SCORE_PROP_NUMBER_BELONGING_INDIVIDUAL,(int)oldNumber);
            ontoRef.synchronizeReasoner();
            ontoRef.saveOntology(SCORE.SCORE_FILE_PATH);
            scoreSemantic.readSemantic();
            scoreSemantic.addData(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL, scoreBelongingIndividual,true);
            //update number belonging individuals
            scoreSemantic.addData(SCORE.SCORE_PROP_NUMBER_BELONGING_INDIVIDUAL, numberBelongingIndividual,true);
            //update the ontology
            scoreSemantic.writeSemanticInconsistencySafe();
            //ontoRef.synchronizeReasoner();
            scoreSemantic.saveOntology(SCORE.SCORE_FILE_PATH);
            scoreSemantic.readSemantic();
            //computing the new score
            float newScoreSemantic = computeScore(scoreSemantic);
            //update total semantic score
            UpdateTotalSemanticScore(oldScoreSemantic, newScoreSemantic);
            //updating all the relations
            updateAllRelations();
            //updating superClass score
            updateSuperClassScore(superClasses,
                    oldScoreSemantic,
                    newScoreSemantic);
            //updating hte ontology
            scoreSemantic.removeData(SCORE.SCORE_PROP_HAS_SCORE);
            scoreSemantic.addData(SCORE.SCORE_PROP_HAS_SCORE, newScoreSemantic,true);
            scoreSemantic.addObject(SCORE.SCORE_OBJ_PROP_HAS_INDIVIDUAL, episodicName);
            scoreSemantic.writeSemantic();
        }

        /**
         * Function which updates the score of the semantic item to which
         * an episodic score belong when the latter has modified its score
         *
         * @param oldScore old score of the episodic item
         * @param newScore new score of the episodic item
         */
        public void updateSemanticFromIndividual(float oldScore, float newScore) {
            //read the  ontology
            scoreSemantic.readSemantic();
            //storing the old score value
            float oldSemanticScore = scoreSemantic.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat();
            //update scoreBelonginIndividual
            float scoreBelongingIndividual = scoreSemantic.getLiteral(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL).parseFloat();
            float oldScoreBel=scoreBelongingIndividual;
            scoreBelongingIndividual -= oldScore;
            scoreBelongingIndividual += newScore;
            ontoRef.synchronizeReasoner();
            ontoRef.removeDataPropertyB2Individual(Name,SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL,oldScoreBel);
            ontoRef.synchronizeReasoner();
            ontoRef.saveOntology(SCORE.SCORE_FILE_PATH);
            scoreSemantic.readSemantic();
            scoreSemantic.saveOntology(SCORE.SCORE_FILE_PATH);
            scoreSemantic.addData(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL, scoreBelongingIndividual,true);
            scoreSemantic.writeSemanticInconsistencySafe();
            ontoRef.synchronizeReasoner();
            scoreSemantic.saveOntology(SCORE.SCORE_FILE_PATH);
            //computing the new score
            float newScoreSemantic = computeScore(scoreSemantic);
            //updating the ontology with the new score
            scoreSemantic.removeData(SCORE.SCORE_PROP_HAS_SCORE);
            scoreSemantic.writeSemantic();
            ontoRef.synchronizeReasoner();
            scoreSemantic.readSemantic();
            scoreSemantic.addData(SCORE.SCORE_PROP_HAS_SCORE, newScoreSemantic);
            scoreSemantic.writeSemantic();
            ontoRef.synchronizeReasoner();
            scoreSemantic.saveOntology(SCORE.SCORE_FILE_PATH);
            //updating the total semantic score
            UpdateTotalSemanticScore(oldSemanticScore, newScoreSemantic);
            //update the superClasses
            updateAllRelations();
            updateSuperClassScore(getSuperClasses(),oldSemanticScore,
                    newScoreSemantic);


        }
        /**
         * function which forgets the semantic score item
         */
        public List<String> forgetItem() {
            ontoRef.synchronizeReasoner();
            //read the current ontology state
            scoreSemantic.readSemantic();
            //storing the score value
            float score=scoreSemantic.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat();
            //Remove its score from the total semantic score
            UpdateTotalSemanticScore(score, 0);
            BelongingIndividual();
            //delete the belonging individuals
            for (String s : belongingIndividuals) {
                EpisodicScore ep = new EpisodicScore(s, ontoRef);
                ep.forgetItem();
            }
            //deleting from the superClasses
            deleteFromSuperClasses();
            ontoRef.removeIndividual(this.Name);
            return  belongingIndividuals;
        }

        /**
         * function which update the object list of belonging individuals
         */
        private void BelongingIndividual() {
            belongingIndividuals.clear();
            ontoRef.synchronizeReasoner();
            scoreSemantic.readSemantic();
            memory.objectPropertyValues(scoreSemantic.getObjectSemantics(), SCORE.SCORE_OBJ_PROP_HAS_INDIVIDUAL, belongingIndividuals,SCORE.SCORE_IRI_ONTO);

        }

        /**
         * function which update the object list of super classes
         */
        private void SuperClasses() {
            superClasses.clear();
            scoreSemantic.readSemantic();
            memory.objectPropertyValues(scoreSemantic.getObjectSemantics(), SCORE.SCORE_OBJ_PROP_IS_SUB_CLASS_OF, superClasses,SCORE.SCORE_IRI_ONTO);
            superClasses.remove(CLASS.SCENE);
        }

        /**
         * function which update the object list of subclasses
         */
        private void SubClasses() {
            subClasses.clear();
            ontoRef.synchronizeReasoner();
            scoreSemantic.readSemantic();
            memory.objectPropertyValues(scoreSemantic.getObjectSemantics(), SCORE.SCORE_OBJ_PROP_IS_SUPER_CLASS_OF, subClasses,SCORE.SCORE_IRI_ONTO);
        }

        /**
         * function which update the Object first super class name
         */
        private void FirstSuperClass() {
            scoreSemantic.readSemantic();
            memory.objectPropertyValues(scoreSemantic.getObjectSemantics(), SCORE.SCORE_OBJ_PROP_FIRST_SUPERCLASS, this.firstSuperClass,SCORE.SCORE_IRI_ONTO);
        }

        /**
         * function which update the Object first superClass NAME
         */
        private void IsFirstSuperClassOf() {
            scoreSemantic.readSemantic();
            memory.objectPropertyValues(scoreSemantic.getObjectSemantics(), SCORE.SCORE_OBJ_PROP_IS_FIRST_SUPER_CLASS_OF,
                    isFirstSuperCLassOf,SCORE.SCORE_IRI_ONTO);
        }

        /**
         * function which update all the object paramenters
         */
        public void updateAllRelations() {
            BelongingIndividual();
            SuperClasses();
            SubClasses();
            //FirstSuperClass();
            //IsFirstSuperClassOf();
        }

        /**
         * @return the List of Sub Classes
         */
        public List<String> getSubClasses() {
            return this.subClasses;
        }

        /**
         * @return the list of SuperClasses
         */
        public List<String> getSuperClasses() {
            return this.superClasses;
        }

        /**
         * @return the list of Belonging Individuals
         */
        public List<String> getBelongingIndividuals() {
            return this.belongingIndividuals;
        }

        /**
         * @return the String containing the name of the fist super class
         */
        public List<String> getFirstSuperClass() {
            return this.firstSuperClass;
        }

        /**
         * @return the Individual score Semantic
         */
        public MORFullIndividual getScoreSemantic() {
            return this.scoreSemantic;
        }

        /**
         * @return the individual total Score Semantic
         */
        public MORFullIndividual getTotalScoreSemantic() {
            return this.totalScoreSemantic;
        }

        /**
         * @return the individual total score Episodic
         */
        public MORFullIndividual getTotalScoreEpisodic() {
            return this.totalScoreEpisodic;
        }

        /**
         * @return the string containing the name of the semantic score individual
         */
        public String getName() {
            return this.Name;
        }


    }

    /**
     * Class which manage the Episodic score initialization, update and removal;
     */
   class EpisodicScore implements SITBase,MemoryInterface {
        private String Name;
        private SemanticScore SemanticItem;
        MORFullIndividual scoreEpisodic;
        MORFullIndividual totalScoreEpisodic;
        boolean addTime = true;
        private Long time = System.currentTimeMillis();
        private Long timeBeginning ;
        OWLReferences ontoRef;

        /**
         * Constructor , when this constructor is used, it is assumed that the item does not
         * exists in the ontology itself hence it is initialized
         *
         * @param Name         name of the episodic item score
         * @param SemanticName name of the semanti item to which this item belongs
         * @param ontoRef      reference to the ontology to manipulate
         */
        public EpisodicScore(String Name, String SemanticName, OWLReferences ontoRef, boolean addTime) {
            this.Name = Name;
            scoreEpisodic = new MORFullIndividual(Name,
                    ontoRef);
            totalScoreEpisodic = new MORFullIndividual(SCORE.SCORE_INDIVIDUAL_TOTAL_EPISODIC,
                    ontoRef);
            SemanticItem = new SemanticScore(SemanticName, ontoRef);
            MORFullIndividual clock = new MORFullIndividual(TIME.CLOCK, ontoRef);
            ontoRef.synchronizeReasoner();
            clock.readSemantic();
            timeBeginning = (long) clock.getLiteral(SCORE.SCORE_PROP_HAS_TIME).parseFloat();
            clock.readSemantic();
            clock.removeData(SCORE.SCORE_PROP_HAS_TIME);
            clock.addData(SCORE.SCORE_PROP_HAS_TIME,System.currentTimeMillis());
            clock.writeSemantic();
            clock.saveOntology(SCORE.SCORE_FILE_PATH);
            clock.addData(SCORE.SCORE_PROP_HAS_TIME,System.currentTimeMillis());
            clock.writeSemantic();
            clock.saveOntology(SCORE.SCORE_FILE_PATH);
            this.ontoRef = ontoRef;
            this.addTime = addTime;
        }

        /**
         * Constructo,When this constructor is used it is assumed that the episodic score item already
         * exists in the ontology
         *
         * @param Name    Name of the episodic Item
         * @param ontoRef Reference to the ontology to be manipulated
         */
        public EpisodicScore(String Name, OWLReferences ontoRef) {
            this.Name = Name;
            scoreEpisodic = new MORFullIndividual(Name, ontoRef);
            totalScoreEpisodic = new MORFullIndividual(SCORE.SCORE_INDIVIDUAL_TOTAL_EPISODIC, ontoRef);
            MORFullIndividual clock = new MORFullIndividual(TIME.CLOCK, ontoRef);
            ontoRef.synchronizeReasoner();
            clock.readSemantic();
            timeBeginning = (long) clock.getLiteral(SCORE.SCORE_PROP_TIME_BEGINNING).parseFloat();
            this.ontoRef = ontoRef;


        }

        /**
         * Function which initialize the episodic score item
         */
        public void episodicInitialization() {
            ontoRef.synchronizeReasoner();
            scoreEpisodic.readSemantic();
            scoreEpisodic.addTypeIndividual(SCORE.SCORE_CLASS_EPISODIC_SCORE);
            // add the corresponding data properties
            System.out.println("added individual to the class " + SCORE.SCORE_CLASS_EPISODIC_SCORE);
            scoreEpisodic.addData(SCORE.SCORE_PROP_NUMBER_EPISODIC_RETRIEVAL, 1);
            scoreEpisodic.addData(SCORE.SCORE_PROP_NUMBER_SEMANTIC_RETRIEVAL, 0);
            scoreEpisodic.addData(SCORE.SCORE_PROP_TIMES_FORGOTTEN, 0);
            scoreEpisodic.addData(SCORE.SCORE_PROP_TIMES_LOW_SCORE, 0);
            scoreEpisodic.addData(SCORE.SCORE_PROP_USER_NO_FORGET, false, true);
            if (addTime) {
                scoreEpisodic.addData(SCORE.SCORE_PROP_HAS_TIME, time);
            }
            System.out.println("setting data property...");
            //Add obj Property
            scoreEpisodic.addObject(SCORE.SCORE_OBJ_PROP_IS_INDIVIDUAL_OF, SemanticItem.getName());
            System.out.println("setting  object property...");
            //compute the score and add it to the individual
            float scoreComputed = computeScore(0, 1,time,timeBeginning);
            scoreEpisodic.addData(SCORE.SCORE_PROP_HAS_SCORE, scoreComputed);
            System.out.println("setting and added score...");
            //write the semantic
            scoreEpisodic.writeSemantic();
            ontoRef.synchronizeReasoner();
            scoreEpisodic.readSemantic();
            //update the total episodic score
            updateTotalEpisodicScore(scoreComputed);
            //update the semantic score from the individual
            System.out.println("updating semantic from individual...");
            SemanticItem.updateSemanticFromIndividual(Name, scoreComputed);
            ontoRef.synchronizeReasoner();
            scoreEpisodic.saveOntology(SCORE.SCORE_FILE_PATH);
        }

        /**
         * function which update the episodic score if the episodic item has been retrieved
         */
        public void episodicRetrieval() {
            //reading the ontology
            ontoRef.synchronizeReasoner();
            scoreEpisodic.readSemantic();
            //store the score value
            float oldScore= scoreEpisodic.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat();
            //updating number retrievals
            int numberEpisodicRetrieval = scoreEpisodic.getLiteral(
                    SCORE.SCORE_PROP_NUMBER_EPISODIC_RETRIEVAL).parseInteger();
            numberEpisodicRetrieval++;
            //computing the new score
            float newScore = computeScore(scoreEpisodic.getLiteral(SCORE.SCORE_PROP_NUMBER_SEMANTIC_RETRIEVAL).parseInteger(),
                   numberEpisodicRetrieval,(long) scoreEpisodic.getLiteral(SCORE.SCORE_PROP_HAS_TIME).parseFloat(),timeBeginning);
            //updating the total episodic score
            updateTotalEpisodicScore(oldScore,newScore);
            UpdateSemanticItem();
            //update the semantic item
            SemanticItem.updateSemanticFromIndividual(oldScore, newScore);
            //update the ontology
            scoreEpisodic.removeData(SCORE.SCORE_PROP_NUMBER_EPISODIC_RETRIEVAL);
            scoreEpisodic.writeSemantic();
            ontoRef.synchronizeReasoner();
            scoreEpisodic.readSemantic();
            scoreEpisodic.addData(SCORE.SCORE_PROP_NUMBER_EPISODIC_RETRIEVAL, numberEpisodicRetrieval);
            scoreEpisodic.removeData(SCORE.SCORE_PROP_HAS_SCORE);
            scoreEpisodic.writeSemantic();
            ontoRef.synchronizeReasoner();
            scoreEpisodic.readSemantic();
            scoreEpisodic.addData(SCORE.SCORE_PROP_HAS_SCORE, newScore);
            scoreEpisodic.writeSemantic();
            ontoRef.synchronizeReasoner();
            scoreEpisodic.saveOntology(SCORE.SCORE_FILE_PATH);
        }

        /**
         * function which forget the episodic item hence delete it from the ontology
         */
        public void forgetItem() {
            //reading the ontology
            ontoRef.synchronizeReasoner();
            scoreEpisodic.readSemantic();
            //update total episodic score
            updateTotalEpisodicScore(scoreEpisodic.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(), 0);
            //update semantic item
            SemanticItem.deleteEpisodicItem(this.Name, scoreEpisodic.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat());
            //remove individual
            ontoRef.removeIndividual(this.Name);
            ontoRef.synchronizeReasoner();
            //saving the ontology
            ontoRef.saveOntology(SCORE.SCORE_FILE_PATH);

        }

        /**
         * function which update the episodic item score if the class to which
         * it belong has been retrieved
         */
        public ArrayList<Float> episodicSemanticRetrieval() {
            ArrayList<Float> values= new ArrayList<>();
            ontoRef.synchronizeReasoner();
            scoreEpisodic.readSemantic();
            //storing the old score value
            float oldScore = scoreEpisodic.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat();
            values.add(oldScore);
            //changing the number of semantic retrieval
            int semanticRetrieval = scoreEpisodic.getLiteral(SCORE.SCORE_PROP_NUMBER_SEMANTIC_RETRIEVAL).parseInteger();
            semanticRetrieval++;
            //compute the new score
            float newScore = computeScore(
                    semanticRetrieval, scoreEpisodic.getLiteral(SCORE.SCORE_PROP_NUMBER_EPISODIC_RETRIEVAL).parseInteger(),
                    (long)scoreEpisodic.getLiteral(SCORE.SCORE_PROP_HAS_TIME).parseFloat(),timeBeginning);
            values.add(newScore);
            //update the total semantic score
            updateTotalEpisodicScore(oldScore, newScore);
            //update the score of the classes
            //SemanticItem.updateSemanticFromIndividual(oldScore, newScore);
            scoreEpisodic.removeData(SCORE.SCORE_PROP_NUMBER_SEMANTIC_RETRIEVAL);
            scoreEpisodic.writeSemantic();
            ontoRef.synchronizeReasoner();
            scoreEpisodic.readSemantic();
            scoreEpisodic.addData(SCORE.SCORE_PROP_NUMBER_SEMANTIC_RETRIEVAL, semanticRetrieval);
            scoreEpisodic.removeData(SCORE.SCORE_PROP_HAS_SCORE);
            scoreEpisodic.writeSemantic();
            ontoRef.synchronizeReasoner();
            scoreEpisodic.readSemantic();
            scoreEpisodic.addData(SCORE.SCORE_PROP_HAS_SCORE, newScore);
            //update the ontology
            scoreEpisodic.writeSemantic();
            ontoRef.synchronizeReasoner();
            scoreEpisodic.saveOntology(SCORE.SCORE_FILE_PATH);
            return values;
        }


        /**
         * function which update the SemanticItem variable of the class
         */
        private void UpdateSemanticItem() {
            List<String> Names = new ArrayList<>();
            String Name = "";
            ontoRef.synchronizeReasoner();
            scoreEpisodic.readSemantic();
            ontoRef.synchronizeReasoner();
            memory.objectPropertyValues(scoreEpisodic.getObjectSemantics(), SCORE.SCORE_OBJ_PROP_IS_INDIVIDUAL_OF, Names,SCORE.SCORE_IRI_ONTO);
            for (String s : Names) {
                Name = s;
            }
            this.SemanticItem = new SemanticScore(Name, ontoRef);
        }

        /**
         * Function which compute the episodic score
         *
         * @param semantic_retrieval number of semantic retrieval
         * @param episodic_retrieval number of episodic retrieval
         * @return episodic score
         */
        private float computeScore(int semantic_retrieval,
                                   int episodic_retrieval,long time,long timeBeginning) {
            float timeDifferenceInHours=(float)(time-timeBeginning)/TIME.DIVISION_TO_FIND_HOUR;
            float timeContribute;
            if(timeDifferenceInHours==0.0){
                timeContribute=0;
            }
            else{
                timeContribute=(float)Math.log((double)timeDifferenceInHours);
            }
            return ((float) (SCORE.SCORE_EPISODIC_WEIGHT_1 * semantic_retrieval +
                    SCORE.SCORE_EPISODIC_WEIGHT_2 * episodic_retrieval+SCORE.SCORE_EPISODIC_WEIGHT_3*(Math.abs((double)timeContribute))));


        }

        /**
         * function which updated the total episodic score if a new episodic item has been created
         *
         * @param score value of the score of the new episodic item
         */
        public void updateTotalEpisodicScore(float score) {
            //reading the ontology
            ontoRef.synchronizeReasoner();
            totalScoreEpisodic.readSemantic();
            //update the total
            float total = totalScoreEpisodic.getLiteral(SCORE.SCORE_PROP_HAS_VALUE).parseFloat();
            total += score;
            //update the ontology
            totalScoreEpisodic.removeData(SCORE.SCORE_PROP_HAS_VALUE);
            totalScoreEpisodic.writeSemantic();
            ontoRef.synchronizeReasoner();
            totalScoreEpisodic.readSemantic();
            totalScoreEpisodic.addData(SCORE.SCORE_PROP_HAS_VALUE, total);
            totalScoreEpisodic.writeSemantic();
        }

        /**
         * function which updated the total episodic score if the episodic score of
         * an item has been changed
         *
         * @param oldScore old score of the episodic item
         * @param newScore new score of the episodic item
         */
        public void updateTotalEpisodicScore(float oldScore, float newScore) {
            //reading the ontology
            ontoRef.synchronizeReasoner();
            totalScoreEpisodic.readSemantic();
            //update the total
            float total = totalScoreEpisodic.getLiteral(SCORE.SCORE_PROP_HAS_VALUE).parseFloat();
            total -= oldScore;
            total += newScore;
            //update the ontology
            totalScoreEpisodic.removeData(SCORE.SCORE_PROP_HAS_VALUE);
            totalScoreEpisodic.writeSemantic();
            ontoRef.synchronizeReasoner();
            totalScoreEpisodic.readSemantic();
            totalScoreEpisodic.addData(SCORE.SCORE_PROP_HAS_VALUE, total);
            totalScoreEpisodic.writeSemantic();

        }



    }

    /**
     * Class which manage the forgetting process
     */
    class Forgetting implements SITBase,MemoryInterface {
        private MORFullConcept forgotClassSemantic;
        private MORFullConcept toBeForgottenClassSemantic;
        private MORFullConcept lowScoreClassSemantic;
        private MORFullConcept forgotClassEpisodic;
        private MORFullConcept toBeForgottenClassEpisodic;
        private MORFullConcept lowScoreClassEpisodic;
        List<String> toBeForgottenSemantic;
        List<String> toBeForgottenEpisodic;
        List<String> lowScoreSemantic;
        List<String> lowScoreEpisodic;
        List<String> forgotSemantic;
        List<String> forgotEpisodic;
        OWLReferences ontoRef;

        public Forgetting(OWLReferences ontoRef) {
            this.ontoRef=ontoRef;
            this.ontoRef.synchronizeReasoner();
            //initializing the class
            toBeForgottenClassEpisodic = new MORFullConcept(SCORE.SCORE_CLASS_EPISODIC_TO_BE_FORGOTTEN,
                    this.ontoRef);
            toBeForgottenClassSemantic = new MORFullConcept(SCORE.SCORE_CLASS_SEMANTIC_TO_BE_FORGOTTEN,
                    this.ontoRef);
            lowScoreClassSemantic = new MORFullConcept(SCORE.SCORE_CLASS_SEMANTIC_LOW_SCORE,
                    this.ontoRef);
            lowScoreClassEpisodic = new MORFullConcept(SCORE.SCORE_CLASS_EPISODIC_LOW_SCORE,
                    this.ontoRef);
            forgotClassEpisodic = new MORFullConcept(SCORE.SCORE_CLASS_FORGOTTEN_EPISODIC,
                    this.ontoRef);
            forgotClassSemantic = new MORFullConcept(SCORE.SCORE_CLASS_FORGOTTEN_SEMANTIC,
                    this.ontoRef);
            MORFullIndividual clock = new MORFullIndividual(TIME.CLOCK,this.ontoRef);
            clock.readSemantic();
            clock.removeData(SCORE.SCORE_PROP_HAS_TIME);
            clock.addData(SCORE.SCORE_PROP_HAS_TIME,System.currentTimeMillis());
            clock.writeSemantic();
            toBeForgottenEpisodic= new ArrayList<>();
            toBeForgottenSemantic= new ArrayList<>();
            lowScoreSemantic= new ArrayList<>();
            lowScoreEpisodic= new ArrayList<>();
            forgotSemantic= new ArrayList<>();
            forgotEpisodic= new ArrayList<>();
            updateLists();

        }

        /**
         * Function which deletes the episodic item stored in the forgot episodic List
         */
        public void deleteEpisodic() {
            //for each episodic element that must be deleted
            for (String name : forgotEpisodic) {
                //declaration of the object
                EpisodicScore ind = new EpisodicScore(name, ontoRef);
                ind.forgetItem();
            }
        }

        /**
         * Function which deletes the semantic item stored in the forgot semantic list
         */
        public void deleteSemantic() {
            //for all the semantic item that must be deleted
            for (String s : forgotSemantic) {
                SemanticScore score = new SemanticScore(s, ontoRef);
                forgotEpisodic.addAll(score.forgetItem());
            }
        }

        /**
         * Function which update the counters related to
         * TimesToBeForgotten
         * TimesLowScore
         */
        public void updateTimes() {
            updateTimes(toBeForgottenEpisodic, SCORE.SCORE_PROP_TIMES_TO_BE_FORGOTTEN);
            updateTimes(toBeForgottenSemantic, SCORE.SCORE_PROP_TIMES_TO_BE_FORGOTTEN);
            updateTimes(lowScoreEpisodic, SCORE.SCORE_PROP_TIMES_LOW_SCORE);
            updateTimes(lowScoreSemantic, SCORE.SCORE_PROP_TIMES_LOW_SCORE);
            ontoRef.synchronizeReasoner();

        }

        /**
         * FUnction which read the ontology and update the list of ToBeForgotten Semantic and Episodi,
         * LowScore semantic and Episodic and Forgot Semantic and Episodic
         */
        public void updateLists() {
            //update the clock
            long time = System.currentTimeMillis();
            MORFullIndividual clock = new MORFullIndividual(TIME.CLOCK,ontoRef);
            ontoRef.synchronizeReasoner();
            clock.readSemantic();
            clock.removeData(SCORE.SCORE_PROP_HAS_TIME);
            clock.addData(SCORE.SCORE_PROP_HAS_TIME,time );
            clock.writeSemantic();
            //syncronize the reasoner
            ontoRef.synchronizeReasoner();
            //clear the Lists
            lowScoreEpisodic.clear();
            lowScoreSemantic.clear();
            forgotEpisodic.clear();
            forgotSemantic.clear();
            toBeForgottenEpisodic.clear();
            toBeForgottenSemantic.clear();
            ontoRef.synchronizeReasoner();
            //read the ontology
            forgotClassSemantic.readSemantic();
            //get the individual classified
            MORAxioms.Individuals indsForgotSemantic = forgotClassSemantic.getIndividualClassified();
            //Adding the individual to the list
            for (OWLNamedIndividual i : indsForgotSemantic) {
                forgotSemantic.add(i.toStringID().substring(SCORE.SCORE_IRI_ONTO.length() + 1));
            }

            //read the ontology
            forgotClassEpisodic.readSemantic();
            //getting the individual classified
            MORAxioms.Individuals indsForgotEpisodic = forgotClassEpisodic.getIndividualClassified();
            //adding the individuals to the list
            for (OWLNamedIndividual i : indsForgotEpisodic) {
                forgotEpisodic.add(i.toStringID().substring(SCORE.SCORE_IRI_ONTO.length() + 1));
            }
            //reading the ontology
            toBeForgottenClassEpisodic.readSemantic();
            //Getting the individual classified
            MORAxioms.Individuals indsForgottenEpisodic = toBeForgottenClassEpisodic.getIndividualClassified();
            //adding the individuals to the List
            for (OWLNamedIndividual i : indsForgottenEpisodic) {
                toBeForgottenEpisodic.add(i.toStringID().substring(SCORE.SCORE_IRI_ONTO.length() + 1));
            }
            //readint the ontology
            toBeForgottenClassSemantic.readSemantic();
            //Getting the individuals classified
            MORAxioms.Individuals indsForgottenSemantic = toBeForgottenClassSemantic.getIndividualClassified();
            //adding the individuals to the list
            for (OWLNamedIndividual i : indsForgottenSemantic) {
                toBeForgottenSemantic.add(i.toStringID().substring(SCORE.SCORE_IRI_ONTO.length() + 1));
            }
            //reading the ontology
            lowScoreClassEpisodic.readSemantic();
            //getting the individual classified
            MORAxioms.Individuals indsLowScoreEpisodic = lowScoreClassEpisodic.getIndividualClassified();
            //adding them to the List
            for (OWLNamedIndividual i : indsLowScoreEpisodic) {
                lowScoreEpisodic.add(i.toStringID().substring(SCORE.SCORE_IRI_ONTO.length() + 1));
            }
            //readint the ontology
           lowScoreClassSemantic.readSemantic();
            //getting the individual classified
            MORAxioms.Individuals indsLowScoreSemantic = lowScoreClassSemantic.getIndividualClassified();
            //Affing the individuals to the list
            for (OWLNamedIndividual i : indsLowScoreSemantic) {
                lowScoreSemantic.add(i.toStringID().substring(SCORE.SCORE_IRI_ONTO.length() + 1));
            }
            //removing the common elements to the lists
            List<String> commonElementsSemanticLowScore= new ArrayList<>();
            commonElementsSemanticLowScore.addAll(toBeForgottenSemantic);
            commonElementsSemanticLowScore.retainAll(lowScoreSemantic);
            lowScoreSemantic.removeAll(commonElementsSemanticLowScore);

            List<String> commonElementsEpisodicLowScore= new ArrayList<>();
            commonElementsEpisodicLowScore.addAll(toBeForgottenEpisodic);
            commonElementsEpisodicLowScore.retainAll(lowScoreEpisodic);
            lowScoreEpisodic.removeAll(commonElementsEpisodicLowScore);


            List<String> commonElementsSemantic= new ArrayList<>();
            commonElementsSemantic.addAll(toBeForgottenSemantic);
            commonElementsSemantic.retainAll(forgotSemantic);
            toBeForgottenSemantic.removeAll(commonElementsSemantic);

            List<String> commonElementsEpisodic= new ArrayList<>();
            commonElementsEpisodic.addAll(toBeForgottenEpisodic);
            commonElementsEpisodic.retainAll(forgotEpisodic);
            toBeForgottenEpisodic.removeAll(commonElementsEpisodic);
        }

        /**
         * Function which update the counters
         * @param names  Name of the item whom coutners must be update
         * @param Property  Name of the property which stores the counter information
         */
        public void updateTimes(List<String> names, String Property) {
            if (names.isEmpty()) {
                System.out.println("nothing to update");
                return;
            }
            //for all the items
            for (String s : names) {
                //definition of the ontological individual
                MORFullIndividual ind = new MORFullIndividual(s,
                        ontoRef);
                //read the ontology
                ontoRef.synchronizeReasoner();
                ind.readSemantic();
                //udpate the counter
                int number = ind.getLiteral(
                        Property).parseInteger();
                number++;
                //update the ontology
                ind.removeData(Property);
                ind.addData(Property, number);
                ind.writeSemantic();
            }

        }

        public List<String> getToBeForgottenSemantic() {
            return this.toBeForgottenSemantic;
        }

        public List<String> getToBeForgottenEpisodic() {
            return this.toBeForgottenEpisodic;
        }

        public List<String> getLowScoreSemantic() {
            return this.lowScoreSemantic;
        }

        public List<String> getLowScoreEpisodic() {
            return this.lowScoreEpisodic;
        }

        public List<String> getForgotSemantic() {
            return this.forgotSemantic;
        }

        public List<String> getForgotEpisodic() {
            return this.forgotEpisodic;
        }



    }

    /**
     * Class which define the score counter item
     */
    class ScoreCounter implements SITBase,MemoryInterface {

        private String scoreName;
        private float scoreValue;
        private int counter;


        public ScoreCounter(String name, float scoreValue, int counter) {
            this.scoreName = name;
            this.scoreValue = scoreValue;
            this.counter = counter;
        }

        public ScoreCounter(String name, float scoreValue) {
            this.scoreName = name;
            this.scoreValue = scoreValue;

        }

        public ScoreCounter(String name) {
            this.scoreName = name;
        }
        public String getScoreName() {
            return scoreName;
        }

        public int getCounter() {
            return counter;
        }

        public float getScoreValue() {
            return scoreValue;
        }
        public void  setCounter(int counter){this.counter=counter;}
        public void setScoreValue (float scoreValue){this.scoreValue=scoreValue;}

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ScoreCounter)) return false;
            ScoreCounter that = (ScoreCounter) o;
            return getScoreName().equals(that.getScoreName()) &&
                    getScoreValue() == that.getScoreValue() &&
                    getCounter() == that.getCounter();
        }
        /**
         * It is used to implement {@link #equals(Object)} method.
         * @return a hash code value for this object.
         */
        //TODO
        //@Override
        //public int hashCode() {
        //return getObjectId() != null ? getObjectId().hashCode() : 0;
        // }
        /**
         * @return the textual description of this spatial relation.
         */
        //@Override
        //public String toString() {
        //   return objectId + ":'" + shape + "' ";
        //}


    }

    /**
     * CLass which implements the score counter arrays
     */
    class ScoreCounterArray extends HashSet<ScoreCounter> implements SITBase,MemoryInterface {
        /**
         * Function which allows the ros mapping
         * @param node
         * @return List of sit_msgs.ScoreCOunter
         */
        public ArrayList<sit_msgs.ScoreCounter> mapInROSMsg(ConnectedNode node){
            ArrayList<sit_msgs.ScoreCounter> rosToBeForgettingArray= new ArrayList<sit_msgs.ScoreCounter>();
            for ( ScoreCounter s : this){
                sit_msgs.ScoreCounter rosToBeForgetting=node.getTopicMessageFactory().newFromType( sit_msgs.ScoreCounter._TYPE);
                rosToBeForgetting.setCounter(s.getCounter());
                rosToBeForgetting.setScoreName(s.getScoreName());
                rosToBeForgetting.setScoreValue(s.getScoreValue());
                rosToBeForgettingArray.add(rosToBeForgetting);

            }
            return  rosToBeForgettingArray;

        }


        @Override
        public boolean add(ScoreCounter scoreCounter) {
            // simplify the list by not adding redundant relation
            for ( ScoreCounter a : this)
                if ( a.equals(scoreCounter))
                    return false;
            // add the relation
            return super.add(scoreCounter);
        }

        /**
         * @return the textual description of this set.
         */
        @Override
        public String toString() {
            String out = "\n{";
            int cnt = 0;
            for ( ScoreCounter s : this) {
                out += "\t" + s.toString();
                if( ++cnt < this.size())
                    out += ";\n";
            }
            return out + "}";
        }
    }




}
