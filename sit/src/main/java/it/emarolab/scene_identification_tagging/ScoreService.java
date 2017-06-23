package it.emarolab.scene_identification_tagging;

import it.emarolab.owloop.aMORDescriptor.utility.concept.MORFullConcept;
import javafx.scene.shape.*;
//import jdk.internal.org.objectweb.asm.tree.analysis.Value;
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
import it.emarolab.scene_identification_tagging.owloopDescriptor.SceneClassDescriptor;
import it.emarolab.scene_identification_tagging.realObject.*;
import it.emarolab.scene_identification_tagging.sceneRepresentation.SceneRepresentation;
import it.emarolab.amor.owlInterface.OWLReferences;
import it.emarolab.amor.owlInterface.SemanticRestriction;
import it.emarolab.owloop.aMORDescriptor.MORAxioms;
import it.emarolab.owloop.aMORDescriptor.MORConcept;
import it.emarolab.owloop.aMORDescriptor.utility.MORConceptBase;
import it.emarolab.owloop.aMORDescriptor.utility.individual.MORFullIndividual;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.awt.image.AreaAveragingScaleFilter;
import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Vector;
import java.util.Arrays;

public class ScoreService extends AbstractNodeMain
        implements SITBase {
    private static final String SERVICE_NAME = "ScoreService";

    public boolean initParam(ConnectedNode node) {

        // stat the service
        node.newServiceServer(
                getServerName(), // set service name
                ScoreInterface._TYPE, // set ROS service message
                getService(node) // set ROS service response
        );
        return true;
    }


    public String getServerName() {
        return SERVICE_NAME;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(getServerName());
    }

    @Override
    public void onStart(ConnectedNode node) {
        super.onStart(node);
        // get ROS parameter
        if (!initParam(node))
            System.exit(1);
    }


    /**
     * @param node the bridge to the standard ROS service
     * @return the object that defines the computation to be performed during service call.
     */

    public ServiceResponseBuilder<ScoreInterfaceRequest, ScoreInterfaceResponse> getService(ConnectedNode node) {
        return new ServiceResponseBuilder<ScoreInterfaceRequest, ScoreInterfaceResponse>() {

            /**
             * This object is used to react to {@link ScoreService} call,
             * it defines the computation to be performed.
             *
             * @param request  an initialised ROS message for the server request
             * @param response the ROS message server response, to be set.
             */
            @Override
            public void
            build(ScoreInterfaceRequest request, ScoreInterfaceResponse response) {

                EpisodicScoreItem episodic=request.getEpisodic();
                SemanticScoreItem semantic=request.getSemantic();
                OWLReferences ontoRef = OWLReferencesInterface.OWLReferencesContainer.newOWLReferenceFromFileWithPellet(
                        SCORE.SCORE_ONTO_NAME, SCORE.SCORE_FILE_PATH, SCORE.SCORE_IRI_ONTO, true);
                // suppress aMOR log
                it.emarolab.amor.owlDebugger.Logger.setPrintOnConsole( false);
                if(!semantic.getName().isEmpty()) {
                    SemanticScore semanticScore = new SemanticScore(semantic.getName(), semantic.getSubClasses(), semantic.getSuperClasses(),semantic.getFirstSuperClass(),ontoRef);
                    //if initialization
                    semanticScore.semanticInitialization();
                    //if retrieval
                    semanticScore.semanticRetrieval();
                }
                if(!episodic.getName().isEmpty()){
                    EpisodicScore episodicScore= new EpisodicScore(episodic.getName(),episodic.getNameSemanticItem(),ontoRef);
                    //if initialization
                    episodicScore.episodicInitialization();
                    //if retrieval
                    episodicScore.episodicRetrieval();

                }

                Forgetting forgetting= new Forgetting(ontoRef);
                List<String> toBeForgottenSemantic=forgetting.getToBeForgottenSemantic();
                List<String> toBeForgottenEpisodic=forgetting.getToBeForgottenEpisodic();
                List<String> lowScoreSemantic=forgetting.getLowScoreSemantic();
                List<String> lowScoreEpisodic=forgetting.getLowScoreEpisodic();
                List<String> forgotSemantic=forgetting.getForgotSemantic();
                List<String> forgotEpisodic=forgetting.getForgotEpisodic();
                forgetting.deleteEpisodic();
                forgetting.deleteSemantic();
                forgetting.updateTimes();

            }



        };


    }
    private class SemanticScore {
        private String Name;
        private String firstSuperClass;
        private List<String> subClasses;
        private List<String> superClasses;
        private List<String> belongingIndividuals;
        MORFullIndividual scoreSemantic;
        MORFullIndividual totalScoreSemantic;
        MORFullIndividual totalScoreEpisodic;
        OWLReferences ontoRef;

        public SemanticScore(String Name, List<String> SubClasses, List<String> SuperClasses, String firstSuperClass, OWLReferences ontoRef) {
            scoreSemantic = new MORFullIndividual(Name,
                    ontoRef);
            totalScoreSemantic = new MORFullIndividual(SCORE.SCORE_INDIVIDUAL_TOTAL_SEMANTIC,
                    ontoRef);
            totalScoreEpisodic = new MORFullIndividual(SCORE.SCORE_INDIVIDUAL_TOTAL_EPISODIC,
                    ontoRef);
            this.ontoRef = ontoRef;
            this.Name = Name;
            this.subClasses = SubClasses;
            this.superClasses = SuperClasses;
            this.firstSuperClass = firstSuperClass;
        }

        public SemanticScore(String Name, OWLReferences ontoRef) {
            scoreSemantic = new MORFullIndividual(Name,
                    ontoRef);
            totalScoreSemantic = new MORFullIndividual(SCORE.SCORE_INDIVIDUAL_TOTAL_SEMANTIC,
                    ontoRef);
            totalScoreEpisodic = new MORFullIndividual(SCORE.SCORE_INDIVIDUAL_TOTAL_EPISODIC,
                    ontoRef);
            this.ontoRef = ontoRef;
            this.Name = Name;

        }

        public void semanticInitialization() {
            // add the individual to the class
            System.out.println("added individual to the class " + SCORE.SCORE_CLASS_SEMANTIC_SCORE);
            scoreSemantic.readSemantic();
            scoreSemantic.addTypeIndividual(SCORE.SCORE_CLASS_SEMANTIC_SCORE);
            scoreSemantic.writeSemantic();
            scoreSemantic.readSemantic();
            // add the corresponding data properties
            //TODO you can define an individual score which does this by itself(connected to the ontology)
            System.out.println("Adding data properties ...");
            scoreSemantic.addData(SCORE.SCORE_PROP_NUMBER_BELONGING_INDIVIDUAL, 0);
            scoreSemantic.addData(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL, 0.0);
            scoreSemantic.addData(SCORE.SCORE_PROP_NUMBER_RETRIEVAL, 1);
            scoreSemantic.addData(SCORE.SCORE_PROP_NUMBER_SUB_CLASSES, subClasses.size());
            scoreSemantic.addData(SCORE.SCORE_PROP_SCORE_SUM_SUB_CLASSES, computeSubClassesScore());
            scoreSemantic.addData(SCORE.SCORE_PROP_TIMES_FORGOTTEN, 0);
            scoreSemantic.addData(SCORE.SCORE_PROP_TIMES_LOW_SCORE, 0);
            scoreSemantic.addData(SCORE.SCORE_PROP_USER_NO_FORGET, false, true);
            scoreSemantic.writeSemantic();
            scoreSemantic.readSemantic();
            //compute the score
            System.out.println("Computing and adding the score...");
            float scoreComputed = computeScore(scoreSemantic);
            //add the score to the individual
            scoreSemantic.addData(SCORE.SCORE_PROP_HAS_SCORE, scoreComputed);
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
            scoreSemantic.addObject(SCORE.SCORE_OBJ_PROP_FIRST_SUPERCLASS, this.firstSuperClass);
            scoreSemantic.writeSemantic();
            scoreSemantic.saveOntology(SCORE.SCORE_FILE_PATH);
            //updating super class score
            System.out.println("updating super classes score...");
            updateSuperClassScore(superClasses, scoreComputed);
        }

        public void semanticRetrieval() {
            scoreSemantic.readSemantic();
            BelongingIndividual();
            //find the individual that have been retrieved with the
            // semantic item
            //updating the belonging individual score
            for (String s : belongingIndividuals) {
                EpisodicScore ep = new EpisodicScore(s, ontoRef);
                ep.episodicSemanticRetrieval();
            }
            int retrieval = scoreSemantic.getLiteral(SCORE.SCORE_PROP_NUMBER_RETRIEVAL).parseInteger();
            retrieval++;
            scoreSemantic.removeData(SCORE.SCORE_PROP_NUMBER_RETRIEVAL);
            scoreSemantic.addData(SCORE.SCORE_PROP_NUMBER_RETRIEVAL, retrieval);
            scoreSemantic.writeSemantic();
            float newScore = computeScore(scoreSemantic);
            UpdateTotalSemanticScore(scoreSemantic.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(), newScore);
            SuperClasses();
            updateSuperClassScore(superClasses, scoreSemantic.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(),
                    newScore);
            scoreSemantic.removeData(SCORE.SCORE_PROP_HAS_SCORE);
            scoreSemantic.addData(SCORE.SCORE_PROP_HAS_SCORE, newScore);
            scoreSemantic.writeSemantic();
        }

        private void BelongingIndividual() {
            objectPropertyValues(scoreSemantic.getObjectSemantics(), SCORE.SCORE_OBJ_PROP_HAS_INDIVIDUAL, belongingIndividuals);

        }

        private void SuperClasses() {
            objectPropertyValues(scoreSemantic.getObjectSemantics(), SCORE.SCORE_OBJ_PROP_IS_SUB_CLASS_OF, superClasses);
        }

        //function which compute the sum of the score of the subclasses
        public float computeSubClassesScore() {
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

        private float computeScore(MORFullIndividual ind) {
            //read the current state of the ontology
            ind.readSemantic();
            totalScoreSemantic.readSemantic();
            totalScoreEpisodic.readSemantic();
            float scoreSubClasses;
            float scoreIndividual;
            int retrieval = ind.getLiteral(SCORE.SCORE_PROP_NUMBER_RETRIEVAL).parseInteger();
            int numberBelongingIndividual = ind.getLiteral(SCORE.SCORE_PROP_NUMBER_BELONGING_INDIVIDUAL).parseInteger();
            int numberSubClasses = ind.getLiteral(SCORE.SCORE_PROP_NUMBER_SUB_CLASSES).parseInteger();
            //if the total semantic is equal to 0
            //TODO alla fine hai deciso di dividerlo per il numero di persone ecc perchè cosi e ceoerente se no in qualunque caso
            //sarebbe stato uno score sbagliato che avresti dovuto computare ogni volta che cambiava qualunque cosa
            // l'unica soluzione potrebbe essere computare lo score direttamente antraverso una SWRL rule
            if (numberSubClasses == 0) {
                scoreSubClasses = 0;
            } else {
                scoreSubClasses = ind.getLiteral(SCORE.SCORE_PROP_SCORE_SUM_SUB_CLASSES).parseFloat() / numberSubClasses;
            }
            // if the total episodic is equal to 0
            if (numberBelongingIndividual == 0) {
                scoreIndividual = 0;
            } else {
                scoreIndividual = ind.getLiteral(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL).parseFloat() / numberBelongingIndividual;
            }
            //removing the data that have to be computed by the reasoner
            ind.removeData(SCORE.SCORE_PROP_SCORE_SUB_CLASSES);
            ind.removeData(SCORE.SCORE_PROP_SCORE_BELONGING_INDIVIDUAL);
            ind.writeSemantic();
            return ((float) (SCORE.SCORE_SEMANTIC_WEIGHT_1 * numberBelongingIndividual +
                    SCORE.SCORE_SEMANTIC_WEIGHT_2 * scoreIndividual +
                    SCORE.SCORE_SEMANTIC_WEIGHT_3 * numberSubClasses +
                    SCORE.SCORE_SEMANTIC_WEIGHT_4 * scoreSubClasses +
                    SCORE.SCORE_SEMANTIC_WEIGHT_5 * retrieval));
        }

        ;

        //update the total semantic score  when a new item is added
        //input: score of the semantic item added
        public void UpdateTotalSemanticScore(float scoreComputed) {
            //read the current state of the total semnatic score
            totalScoreSemantic.readSemantic();
            //reading the data property has value
            float oldTotal = totalScoreSemantic.getLiteral(SCORE.SCORE_PROP_HAS_VALUE).parseFloat();
            if (oldTotal == 0.0) {
                oldTotal = scoreComputed;
            } else {
                //change the value by adding the new score
                oldTotal += scoreComputed;
            }
            //change the dataproperty value
            totalScoreSemantic.removeData(SCORE.SCORE_PROP_HAS_VALUE);
            totalScoreSemantic.writeSemantic();
            totalScoreSemantic.addData(SCORE.SCORE_PROP_HAS_VALUE, oldTotal);
            totalScoreSemantic.writeSemantic();

        }

        //update the total semantic score when a score of an item has been changed
        //inputs :
        //-old score of the semantic item modified
        //-new score of the semantic item modified
        public void UpdateTotalSemanticScore(float oldScore, float newScore) {
            //read the current state of total semantic item
            totalScoreSemantic.readSemantic();
            //reading the value of hasValue dataproperty
            float total = totalScoreSemantic.getLiteral(SCORE.SCORE_PROP_HAS_VALUE).parseFloat();
            //updating the value
            total -= oldScore;
            total += newScore;
            //updating the data property with the new value just computed
            totalScoreSemantic.removeData(SCORE.SCORE_PROP_HAS_VALUE);
            totalScoreSemantic.writeSemantic();
            totalScoreSemantic.addData(SCORE.SCORE_PROP_HAS_VALUE, total);
            totalScoreSemantic.writeSemantic();
        }

        //updating superclasses score when a score of subclass has been changed
        //inputs:
        //-names of the superclasses to be updated
        //-old score of the updated semantic score
        //-new score of the updated semantic score
        public void updateSuperClassScore(List<String> classesNames, float scoreOld, float scoreNew) {
            //if the set of string is empty hence there is no super class the functions
            //automatically returns
            if (classesNames.isEmpty()) {
                return;
            }
            //for all the string
            for (String name : classesNames) {
                //define the MOR individual of such superclass
                MORFullIndividual superClass = new MORFullIndividual(
                        name,
                        ontoRef
                );
                //read the ontology
                superClass.readSemantic();
                //update the subclasses score with the new one
                float scoreSubClasses = superClass.getLiteral(SCORE.SCORE_PROP_SCORE_SUM_SUB_CLASSES).parseFloat();
                scoreSubClasses -= scoreOld;
                scoreSubClasses += scoreNew;
                superClass.removeData(SCORE.SCORE_PROP_SCORE_SUM_SUB_CLASSES);
                superClass.addData(SCORE.SCORE_PROP_SCORE_SUM_SUB_CLASSES, scoreSubClasses);
                superClass.writeSemantic();
                //compute the new score
                float newScore = computeScore(superClass);
                //store the old score
                float oldScore = superClass.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat();
                //change the value of the data prop score
                superClass.removeData(SCORE.SCORE_PROP_HAS_SCORE);
                superClass.addData(SCORE.SCORE_PROP_HAS_SCORE, newScore);
                //write the semantic
                superClass.writeSemantic();
                superClass.readSemantic();
                //find the super classes of such element
                MORAxioms.ObjectSemantics objProp = superClass.getObjectSemantics();
                //check if there is any superclasses
                List<String> classes = new ArrayList<>();
                objectPropertyValues(objProp, SCORE.SCORE_OBJ_PROP_IS_SUB_CLASS_OF, classes);
                //update total semantic score
                UpdateTotalSemanticScore(oldScore, newScore);
                //update superclasses score
                updateSuperClassScore(classes, oldScore, newScore);
            }
        }

        //update superclasses score when a new subclass has been added
        //inputs :
        //-name of the superclasses to be updated
        //-score of the semantic item added
        public void updateSuperClassScore(List<String> setName, float score) {
            //if the set of string is empty hence there is no super class the functions
            //automatically returns

            if (setName.isEmpty()) {
                return;
            }
            //for all the string
            for (String name : setName) {
                //define the MOR individual of such superclass
                MORFullIndividual superClass = new MORFullIndividual(
                        name,
                        ontoRef
                );
                //read the ontology
                superClass.readSemantic();
                //update the subclasses score with the new one
                float scoreSubClasses = superClass.getLiteral(SCORE.SCORE_PROP_SCORE_SUM_SUB_CLASSES).parseFloat();
                scoreSubClasses += score;
                superClass.removeData(SCORE.SCORE_PROP_SCORE_SUM_SUB_CLASSES);
                superClass.addData(SCORE.SCORE_PROP_SCORE_SUM_SUB_CLASSES, scoreSubClasses);
                int numberSubClasses = superClass.getLiteral(SCORE.SCORE_PROP_NUMBER_SUB_CLASSES).parseInteger();
                numberSubClasses++;
                superClass.removeData(SCORE.SCORE_PROP_NUMBER_SUB_CLASSES);
                superClass.addData(SCORE.SCORE_PROP_NUMBER_SUB_CLASSES, numberSubClasses);
                superClass.writeSemantic();
                //compute the new score
                float newScore = computeScore(superClass);
                //store the old score
                float oldScore = superClass.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat();
                //change the value of the data prop score
                superClass.removeData(SCORE.SCORE_PROP_HAS_SCORE);
                superClass.addData(SCORE.SCORE_PROP_HAS_SCORE, newScore);
                //write the semantic
                superClass.writeSemantic();
                superClass.readSemantic();
                //find the super classes of such element
                MORAxioms.ObjectSemantics objProp = superClass.getObjectSemantics();
                //check if there is any subclasses
                List<String> classes = new ArrayList<>();
                objectPropertyValues(objProp, SCORE.SCORE_OBJ_PROP_IS_SUB_CLASS_OF, classes);
                //update total semantic score
                UpdateTotalSemanticScore(oldScore, newScore);
                //update superclasses score
                updateSuperClassScore(classes, oldScore, newScore);
            }
        }

        public void deleteEpisodicItem(String s, float score) {
            ontoRef.synchronizeReasoner();
            scoreSemantic.readSemantic();
            scoreSemantic.removeObject(SCORE.SCORE_OBJ_PROP_HAS_INDIVIDUAL, s);
            int numberOfBelongingIndividual = scoreSemantic.getLiteral(SCORE.SCORE_PROP_NUMBER_BELONGING_INDIVIDUAL).parseInteger() - 1;
            float scoreOfBelongingIndividual = scoreSemantic.getLiteral(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL).parseFloat() - score;
            scoreSemantic.removeData(SCORE.SCORE_PROP_NUMBER_BELONGING_INDIVIDUAL);
            scoreSemantic.addData(SCORE.SCORE_PROP_NUMBER_BELONGING_INDIVIDUAL, numberOfBelongingIndividual);
            scoreSemantic.removeData(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL);
            scoreSemantic.addData(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL, scoreOfBelongingIndividual);
            scoreSemantic.writeSemantic();
            float newScore = computeScore(scoreSemantic);
            UpdateTotalSemanticScore(scoreSemantic.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(), newScore);
            SuperClasses();
            updateSuperClassScore(superClasses, scoreSemantic.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(), newScore);
            scoreSemantic.removeData(SCORE.SCORE_PROP_HAS_SCORE);
            scoreSemantic.addData(SCORE.SCORE_PROP_HAS_SCORE, newScore);
            scoreSemantic.writeSemantic();
        }
        public void forgetItem() {
            ontoRef.synchronizeReasoner();
            //read the current ontology state
            scoreSemantic.readSemantic();
            //Remove its score from the total semantic score
            UpdateTotalSemanticScore(scoreSemantic.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(), 0);
            //definition of the Sets for
            //FirstsuperClasses
            //TODO change such that it is updated automatically
            BelongingIndividual();
            //TODO change
            // if there is no super class
            if (firstSuperClass.isEmpty()) {
                //delete the belonging individuals
                for (String s : belongingIndividuals) {
                    MORFullIndividual ep = new MORFullIndividual(s, ontoRef);
                    ep.readSemantic();
                    deleteEpisodicItem(s, ep.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat());
                }
                //TODO change so that it is automatic
                //remove first superClass attribute from its subclass
                List<String> isFirstSuperClassOf = new ArrayList<>();
                //taking from the ontology info about which items the semantic item was first class of
                objectPropertyValues(scoreSemantic.getObjectSemantics(), SCORE.SCORE_OBJ_PROP_IS_FIRST_SUPER_CLASS_OF, isFirstSuperClassOf);
                //for all such individuals
                //TODO change so that it is a function of the class itself
                for (String sub : isFirstSuperClassOf) {
                    //declare the obeject
                    MORFullIndividual subCl = new MORFullIndividual(sub,
                            ontoRef);
                    //read the current ontology state
                    subCl.readSemantic();
                    //remove the reference to the semantic item
                    subCl.removeObject(SCORE.SCORE_OBJ_PROP_FIRST_SUPERCLASS, this.Name);
                    //write in the ontology
                    subCl.writeSemantic();

                }
            }
            //if there exist the first class --> Hyp it is unique
            else {
                //All its individual will belong to the first superclass
                //for all the individuals
                //TODO do it inside the episodic item
                for (String i : belongingIndividuals) {
                    //declaration of the object
                    MORFullIndividual individual = new MORFullIndividual(i,
                            ontoRef
                    );
                    //read the current ontology state
                    individual.readSemantic();
                    //delete the information about the semantic item that must be deleted
                    individual.removeObject(SCORE.SCORE_OBJ_PROP_IS_INDIVIDUAL_OF, this.Name);
                    //make it belong to the first super class
                    individual.addObject(SCORE.SCORE_OBJ_PROP_IS_INDIVIDUAL_OF, this.firstSuperClass);
                    //write the semantic
                    individual.writeSemantic();
                }
                //update the score of the first superClass
                MORFullIndividual firstSup = new MORFullIndividual(this.firstSuperClass, ontoRef);
                //read the current state of the ontology
                firstSup.readSemantic();
                //update the number of belonging individuals
                int numberBelongingIndividual = firstSup.getLiteral(SCORE.SCORE_PROP_NUMBER_BELONGING_INDIVIDUAL).parseInteger() +
                        scoreSemantic.getLiteral(SCORE.SCORE_PROP_NUMBER_BELONGING_INDIVIDUAL).parseInteger();
                //update the score of belonging individuals
                float scoreBelongingIndividual = firstSup.getLiteral(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL).parseFloat() +
                        scoreSemantic.getLiteral(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL).parseFloat();
                //compute the newScore
                firstSup.removeData(SCORE.SCORE_PROP_NUMBER_BELONGING_INDIVIDUAL);
                firstSup.addData(SCORE.SCORE_PROP_NUMBER_BELONGING_INDIVIDUAL, numberBelongingIndividual);
                firstSup.removeData(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL);
                firstSup.addData(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL, scoreBelongingIndividual);
                firstSup.writeSemantic();
                float newScore = computeScore(firstSup);
                //update the total semantic score
                UpdateTotalSemanticScore(firstSup.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(), newScore);
                //update superClasses score
                List<String> superCl = new ArrayList<>();
                objectPropertyValues(firstSup.getObjectSemantics(), SCORE.SCORE_OBJ_PROP_IS_SUB_CLASS_OF, superCl);
                updateSuperClassScore(superCl,firstSup.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(), newScore);
                //update the ontology
                firstSup.removeData(SCORE.SCORE_PROP_HAS_SCORE);
                firstSup.addData(SCORE.SCORE_PROP_HAS_SCORE, newScore);
                firstSup.writeSemantic();
                //this is done even if there is no superclass
                //REMOVE THE SCORE AND THE CLASS TO THE OTHER CLASSES
                //definition of the set which will contain all the superclass of the semantic item to be deleted
                SuperClasses();
                //for all the superclasses
                for (String sup : this.superClasses)
                {
                    //declare the object
                    MORFullIndividual Sup = new MORFullIndividual(sup,ontoRef);
                    //read the current ontology state
                    Sup.readSemantic();
                    //remove the objcet actribut is superclass of
                    Sup.removeObject(SCORE.SCORE_OBJ_PROP_IS_SUPER_CLASS_OF,this.Name);
                    //update the number of subclass
                    int numberSubClasses = Sup.getLiteral(SCORE.SCORE_PROP_NUMBER_SUB_CLASSES).parseInteger();
                    numberSubClasses--;
                    //update the score of subclass
                    float scoreSubClasses = Sup.getLiteral(SCORE.SCORE_PROP_SCORE_SUM_SUB_CLASSES).parseFloat();
                    scoreSubClasses -= scoreSemantic.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat();
                    Sup.removeData(SCORE.SCORE_PROP_NUMBER_SUB_CLASSES);
                    Sup.addData(SCORE.SCORE_PROP_NUMBER_SUB_CLASSES, numberSubClasses);
                    Sup.removeData(SCORE.SCORE_PROP_SCORE_SUM_SUB_CLASSES);
                    Sup.addData(SCORE.SCORE_PROP_SCORE_SUM_SUB_CLASSES, scoreSubClasses);
                    Sup.writeSemantic();
                    //compute the new score
                    float newScoreSub = computeScore(Sup);
                    //update the semantic with the new score
                    UpdateTotalSemanticScore(Sup.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(), newScoreSub);
                    //update the ontology
                    Sup.removeData(SCORE.SCORE_PROP_HAS_SCORE);
                    Sup.addData(SCORE.SCORE_PROP_HAS_SCORE, newScore);
                    Sup.writeSemantic();
                    //The item which had the semantic item to be deleted as first super class
                    //will now have its first super class as super class
                    //declaration of the Set which will contain all the classes which had the semantic item to be deleted as firstSuperCLass
                    //for all such classes
                    List<String> isFirstSuperClassOf = new ArrayList<>();
                    //taking from the ontology info about which items the semantic item was first class of
                    objectPropertyValues(scoreSemantic.getObjectSemantics(), SCORE.SCORE_OBJ_PROP_IS_FIRST_SUPER_CLASS_OF, isFirstSuperClassOf);
                    for (String sub : isFirstSuperClassOf) {
                        //declaration of the object
                        MORFullIndividual subCl = new MORFullIndividual(sub,
                                ontoRef);
                        //read the current ontology state
                        subCl.readSemantic();
                        //update the first superclass object property
                        subCl.removeObject(SCORE.SCORE_OBJ_PROP_FIRST_SUPERCLASS,this.Name);
                        subCl.addObject(SCORE.SCORE_OBJ_PROP_FIRST_SUPERCLASS,this.firstSuperClass);
                        //write the ontology
                        subCl.writeSemantic();
                    }
                }
            }

        //now that all the individual have been associated to the super class one can delete the individual
            ontoRef.synchronizeReasoner();
            ontoRef.removeIndividual(this.Name);
        }
        public void setSubClasses(List<String> subClasses){this.subClasses=subClasses;}
        public List<String> getSubClasses(){return this.subClasses;}
        public void setSuperClasses(List<String> superClasses){this.superClasses=superClasses;}
        public List<String> getSuperClasses(){return this.superClasses;}
        public MORFullIndividual getScoreSemantic(){return this.scoreSemantic;}
        public MORFullIndividual getTotalScoreSemantic(){return this.totalScoreSemantic;}
        public String getName(){return this.Name;}


    }
    private class EpisodicScore {
        private String Name;
        private SemanticScore SemanticItem;
        private List<String> classes;
        MORFullIndividual scoreEpisodic;
        MORFullIndividual totalScoreEpisodic;
        OWLReferences ontoRef;

        public EpisodicScore(String Name, String SemanticName,OWLReferences ontoRef) {
            this.Name = Name;
            scoreEpisodic = new MORFullIndividual(Name,
                    ontoRef);
            totalScoreEpisodic = new MORFullIndividual(SCORE.SCORE_INDIVIDUAL_TOTAL_EPISODIC,
                    ontoRef);
            SemanticItem = new SemanticScore(SemanticName,ontoRef);
            this.ontoRef=ontoRef;
        }

        public EpisodicScore(String Name, OWLReferences ontoRef){
            this.Name=Name;
            this.ontoRef=ontoRef;
            scoreEpisodic = new MORFullIndividual(Name,ontoRef);
        }

        public void episodicInitialization() {
            scoreEpisodic.readSemantic();
            scoreEpisodic.addTypeIndividual(SCORE.SCORE_CLASS_EPISODIC_SCORE);
            scoreEpisodic.writeSemantic();
            scoreEpisodic.readSemantic();
            //assertSemantic();
            // add the corresponding data properties
            System.out.println("added individual to the class " + SCORE.SCORE_CLASS_EPISODIC_SCORE);
            scoreEpisodic.addData(SCORE.SCORE_PROP_NUMBER_EPISODIC_RETRIEVAL, 1);
            scoreEpisodic.addData(SCORE.SCORE_PROP_NUMBER_SEMANTIC_RETRIEVAL, 0);
            scoreEpisodic.addData(SCORE.SCORE_PROP_TIMES_FORGOTTEN, 0);
            scoreEpisodic.addData(SCORE.SCORE_PROP_TIMES_LOW_SCORE, 0);
            scoreEpisodic.addData(SCORE.SCORE_PROP_USER_NO_FORGET, false, true);
            System.out.println("setting data property...");
            //Add obj Property
            scoreEpisodic.addObject(SCORE.SCORE_OBJ_PROP_IS_INDIVIDUAL_OF, SemanticItem.getName());
            System.out.println("setting  object property...");
            //compute the score and add it to the individual
            float scoreComputed = computeScore(0, 1);
            scoreEpisodic.addData(SCORE.SCORE_PROP_HAS_SCORE, scoreComputed);
            System.out.println("setting and added score...");
            //write the semantic
            scoreEpisodic.writeSemantic();
            scoreEpisodic.readSemantic();
            System.out.println("added data property...");
            scoreEpisodic.writeSemantic();
            System.out.println("added score property...");
            updateTotalEpisodicScore(scoreComputed);
            System.out.println("updating semantic from individual...");
            updateSemanticFromIndividual(Name, scoreComputed);
            scoreEpisodic.saveOntology(SCORE.SCORE_FILE_PATH);
        }
        public void episodicRetrieval(){
            scoreEpisodic.readSemantic();
            int numberEpisodicRetrieval=scoreEpisodic.getLiteral(
                    SCORE.SCORE_PROP_NUMBER_EPISODIC_RETRIEVAL).parseInteger();
            numberEpisodicRetrieval++;
            float newScore=computeScore(scoreEpisodic.getLiteral(SCORE.SCORE_PROP_NUMBER_SEMANTIC_RETRIEVAL).parseInteger(),
                    numberEpisodicRetrieval);
            updateTotalEpisodicScore(scoreEpisodic.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(),
                    newScore);
            Classes();
            for (String s:classes){
                updateSemanticFromIndividual(s,scoreEpisodic.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(),newScore);
            }
            scoreEpisodic.removeData(SCORE.SCORE_PROP_NUMBER_EPISODIC_RETRIEVAL);
            scoreEpisodic.addData(SCORE.SCORE_PROP_NUMBER_EPISODIC_RETRIEVAL,numberEpisodicRetrieval);
            scoreEpisodic.removeData(SCORE.SCORE_PROP_HAS_SCORE);
            scoreEpisodic.addData(SCORE.SCORE_PROP_HAS_SCORE,newScore);
            scoreEpisodic.writeSemantic();
        }

        private float computeScore(int semantic_retrieval,
                                   int episodic_retrieval) {
            return ((float) (SCORE.SCORE_EPISODIC_WEIGHT_1 * semantic_retrieval +
                    SCORE.SCORE_EPISODIC_WEIGHT_2 * episodic_retrieval));


        }

        public void updateTotalEpisodicScore(float score){
            totalScoreEpisodic.readSemantic();
            float total=totalScoreEpisodic.getLiteral(SCORE.SCORE_PROP_HAS_VALUE).parseFloat();
            total += score;
            totalScoreEpisodic.removeData(SCORE.SCORE_PROP_HAS_VALUE);
            totalScoreEpisodic.addData(SCORE.SCORE_PROP_HAS_VALUE,total);
            totalScoreEpisodic.writeSemantic();
        }
        public void updateTotalEpisodicScore(float oldScore,float newScore){
            totalScoreEpisodic.readSemantic();
            float total=totalScoreEpisodic.getLiteral(SCORE.SCORE_PROP_HAS_VALUE).parseFloat();
            total-=oldScore;
            total+=newScore;
            totalScoreEpisodic.removeData(SCORE.SCORE_PROP_HAS_VALUE);
            totalScoreEpisodic.addData(SCORE.SCORE_PROP_HAS_VALUE,total);
            totalScoreEpisodic.writeSemantic();

        }
        public void updateSemanticFromIndividual(String episodicName,float Score){
            MORFullIndividual semanticIndividual=SemanticItem.getScoreSemantic();
            semanticIndividual.readSemantic();
            float scoreBelongingIndividual=semanticIndividual.getLiteral(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL).parseFloat();
            scoreBelongingIndividual+=Score;
            semanticIndividual.removeData(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL);
            semanticIndividual.addData(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL,scoreBelongingIndividual);
            int numberBelongingIndividual=semanticIndividual.getLiteral(SCORE.SCORE_PROP_NUMBER_BELONGING_INDIVIDUAL).parseInteger();
            numberBelongingIndividual++;
            semanticIndividual.removeData(SCORE.SCORE_PROP_NUMBER_BELONGING_INDIVIDUAL);
            semanticIndividual.addData(SCORE.SCORE_PROP_NUMBER_BELONGING_INDIVIDUAL,numberBelongingIndividual);
            semanticIndividual.writeSemantic();
            float newScoreSemantic=SemanticItem.computeScore(semanticIndividual);
            SemanticItem.UpdateTotalSemanticScore(semanticIndividual.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(),newScoreSemantic);
            List<String> classes =new ArrayList<>();
            objectPropertyValues(semanticIndividual.getObjectSemantics(),SCORE.SCORE_OBJ_PROP_IS_SUB_CLASS_OF,classes);
            SemanticItem.updateSuperClassScore(classes,
                    semanticIndividual.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(),
                    newScoreSemantic);
            semanticIndividual.removeData(SCORE.SCORE_PROP_HAS_SCORE);
            semanticIndividual.addData(SCORE.SCORE_PROP_HAS_SCORE,newScoreSemantic);
            semanticIndividual.addObject(SCORE.SCORE_OBJ_PROP_HAS_INDIVIDUAL,episodicName);
            semanticIndividual.writeSemantic();
        }
        public void updateSemanticFromIndividual(float oldScore,float newScore){
            MORFullIndividual semanticIndividual=SemanticItem.getScoreSemantic();
            semanticIndividual.readSemantic();
            float scoreBelongingIndividual=semanticIndividual.getLiteral(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL).parseFloat();
            scoreBelongingIndividual-=oldScore;
            scoreBelongingIndividual+=newScore;
            semanticIndividual.removeData(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL);
            semanticIndividual.addData(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL,scoreBelongingIndividual);
            semanticIndividual.writeSemantic();
            float newScoreSemantic= SemanticItem.computeScore(semanticIndividual);
            SemanticItem.UpdateTotalSemanticScore(semanticIndividual.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(),newScoreSemantic);
            List<String> classes =new ArrayList<>();
            objectPropertyValues(semanticIndividual.getObjectSemantics(),SCORE.SCORE_OBJ_PROP_IS_SUB_CLASS_OF,classes);
            SemanticItem.updateSuperClassScore(classes,semanticIndividual.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(),
                    newScoreSemantic);
            semanticIndividual.removeData(SCORE.SCORE_PROP_HAS_SCORE);
            semanticIndividual.addData(SCORE.SCORE_PROP_HAS_SCORE,newScoreSemantic);
            semanticIndividual.writeSemantic();
        }
        public void updateSemanticFromIndividual(String s,float oldScore,float newScore){
            MORFullIndividual semanticIndividual=new MORFullIndividual(s,ontoRef);
            semanticIndividual.readSemantic();
            float scoreBelongingIndividual=semanticIndividual.getLiteral(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL).parseFloat();
            scoreBelongingIndividual-=oldScore;
            scoreBelongingIndividual+=newScore;
            semanticIndividual.removeData(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL);
            semanticIndividual.addData(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL,scoreBelongingIndividual);
            semanticIndividual.writeSemantic();
            float newScoreSemantic= SemanticItem.computeScore(semanticIndividual);
            SemanticItem.UpdateTotalSemanticScore(semanticIndividual.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(),newScoreSemantic);
            List<String> classes =new ArrayList<>();
            objectPropertyValues(semanticIndividual.getObjectSemantics(),SCORE.SCORE_OBJ_PROP_IS_SUB_CLASS_OF,classes);
            SemanticItem.updateSuperClassScore(classes,semanticIndividual.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(),
                    newScoreSemantic);
            semanticIndividual.removeData(SCORE.SCORE_PROP_HAS_SCORE);
            semanticIndividual.addData(SCORE.SCORE_PROP_HAS_SCORE,newScoreSemantic);
            semanticIndividual.writeSemantic();
        }
        public void episodicSemanticRetrieval(){
                scoreEpisodic.readSemantic();
                //changing the number of semantic retrieval
                int semanticRetrieval=scoreEpisodic.getLiteral(SCORE.SCORE_PROP_NUMBER_SEMANTIC_RETRIEVAL).parseInteger();
                semanticRetrieval++;
                //compute the new score
                float newScore=computeScore(scoreEpisodic.getLiteral(SCORE.SCORE_PROP_NUMBER_EPISODIC_RETRIEVAL).parseInteger(),
                        semanticRetrieval);
                //update the total semantic score
                updateTotalEpisodicScore(scoreEpisodic.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(),newScore);
                Classes();
                //update the score of the classes
                for(String s:classes) {
                    updateSemanticFromIndividual(s,
                            scoreEpisodic.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(),
                            newScore);
                }
                scoreEpisodic.removeData(SCORE.SCORE_PROP_NUMBER_SEMANTIC_RETRIEVAL);
                scoreEpisodic.addData(SCORE.SCORE_PROP_NUMBER_SEMANTIC_RETRIEVAL,semanticRetrieval);
                scoreEpisodic.removeData(SCORE.SCORE_PROP_HAS_SCORE);
                scoreEpisodic.addData(SCORE.SCORE_PROP_HAS_SCORE,newScore);
                scoreEpisodic.writeSemantic();
        }
        private void Classes(){
            scoreEpisodic.readSemantic();
            //checking which classes this individual belongs to
            objectPropertyValues(scoreEpisodic.getObjectSemantics(),SCORE.SCORE_OBJ_PROP_IS_INDIVIDUAL_OF,this.classes);
        }

        public void forgetItem() {
            updateTotalEpisodicScore(scoreEpisodic.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(), 0);
            //declaration of the Set which will contain the name of the classes which this individual belongs to
            Classes();
            //removing the property has individual from the corresponding class
            //for each class, remove the information about the individual
            for (String s : classes) {
               SemanticScore semanticScore= new SemanticScore(s,ontoRef);
               semanticScore.deleteEpisodicItem(this.Name,scoreEpisodic.getLiteral(SCORE.SCORE_PROP_HAS_VALUE).parseFloat());
            }
            ontoRef.removeIndividual(this.Name);
            ontoRef.synchronizeReasoner();

        }
    }
    private class Forgetting {
        private  MORFullConcept forgotClassSemantic;
        private  MORFullConcept toBeForgottenClassSemantic;
        private  MORFullConcept lowScoreClassSemantic;
        private  MORFullConcept forgotClassEpisodic;
        private  MORFullConcept toBeForgottenClassEpisodic;
        private  MORFullConcept lowScoreClassEpisodic;
        List<String> toBeForgottenSemantic;
        List<String> toBeForgottenEpisodic;
        List<String> lowScoreSemantic;
        List<String> lowScoreEpisodic;
        List<String> forgotSemantic;
        List<String> forgotEpisodic;
        OWLReferences ontoRef;

        public Forgetting(OWLReferences ontoRef){
            //initializing the class
            toBeForgottenClassEpisodic= new MORFullConcept(SCORE.SCORE_CLASS_EPISODIC_TO_BE_FORGOTTEN,
                    ontoRef);
            toBeForgottenClassSemantic=new MORFullConcept(SCORE.SCORE_CLASS_SEMANTIC_TO_BE_FORGOTTEN,
                    ontoRef);
            lowScoreClassSemantic=new MORFullConcept(SCORE.SCORE_CLASS_SEMANTIC_LOW_SCORE,
                    ontoRef);
            lowScoreClassEpisodic=new MORFullConcept(SCORE.SCORE_CLASS_EPISODIC_LOW_SCORE,
                    ontoRef);
            forgotClassEpisodic=new MORFullConcept(SCORE.SCORE_CLASS_FORGOTTEN_EPISODIC,
                   ontoRef);
            forgotClassSemantic= new MORFullConcept(SCORE.SCORE_CLASS_FORGOTTEN_SEMANTIC,
                  ontoRef);

            updateLists();

        }
        public void deleteEpisodic() {
            //for each episodic element that must be deleted
            for (String name : forgotEpisodic) {
                //declaration of the object
                EpisodicScore ind = new EpisodicScore(name, ontoRef);
                ind.forgetItem();
            }
        }
        public void deleteSemantic() {
            //for all the semantic item that must be deleted
            for (String s : forgotSemantic) {
                SemanticScore score=new SemanticScore(s,ontoRef);
                score.forgetItem();
            }
        }
        public void updateTimes(){
            updateTimes(toBeForgottenEpisodic,SCORE.SCORE_PROP_TIMES_FORGOTTEN);
            updateTimes(toBeForgottenSemantic,SCORE.SCORE_PROP_TIMES_FORGOTTEN);
            updateTimes(lowScoreEpisodic,SCORE.SCORE_PROP_TIMES_LOW_SCORE);
            updateTimes(lowScoreSemantic,SCORE.SCORE_PROP_TIMES_LOW_SCORE);
            ontoRef.synchronizeReasoner();
            updateLists();
        }
        private void updateLists(){
            ontoRef.synchronizeReasoner();

            forgotClassSemantic.readSemantic();
            MORAxioms.Individuals indsForgotSemantic=forgotClassSemantic.getIndividualClassified();
            for (OWLNamedIndividual i:indsForgotSemantic){
                forgotSemantic.add(i.toStringID().substring(SCORE.SCORE_IRI_ONTO.length() + 1));
            }

            forgotClassEpisodic.readSemantic();
            MORAxioms.Individuals indsForgotEpisodic=forgotClassEpisodic.getIndividualClassified();
            for (OWLNamedIndividual i:indsForgotEpisodic){
                forgotEpisodic.add(i.toStringID().substring(SCORE.SCORE_IRI_ONTO.length() + 1));
            }

            toBeForgottenClassEpisodic.readSemantic();
            MORAxioms.Individuals indsForgottenEpisodic=toBeForgottenClassEpisodic.getIndividualClassified();
            for (OWLNamedIndividual i:indsForgottenEpisodic){
                toBeForgottenEpisodic.add(i.toStringID().substring(SCORE.SCORE_IRI_ONTO.length() + 1));
            }

            toBeForgottenClassSemantic.readSemantic();
            MORAxioms.Individuals indsForgottenSemantic=toBeForgottenClassSemantic.getIndividualClassified();
            for (OWLNamedIndividual i:indsForgottenSemantic){
                toBeForgottenSemantic.add(i.toStringID().substring(SCORE.SCORE_IRI_ONTO.length() + 1));
            }

            lowScoreClassEpisodic.readSemantic();
            MORAxioms.Individuals indsLowScoreEpisodic=lowScoreClassEpisodic.getIndividualClassified();
            for (OWLNamedIndividual i:indsLowScoreEpisodic){
                lowScoreEpisodic.add(i.toStringID().substring(SCORE.SCORE_IRI_ONTO.length() + 1));
            }

            lowScoreClassSemantic.readSemantic();
            MORAxioms.Individuals indsLowScoreSemantic=lowScoreClassSemantic.getIndividualClassified();
            for (OWLNamedIndividual i:indsLowScoreSemantic){
                lowScoreSemantic.add(i.toStringID().substring(SCORE.SCORE_IRI_ONTO.length() + 1));
            }

        }
        public void updateTimes(List<String> names, String Property){
            if(names.isEmpty()){
                System.out.println("nothing to update");
                return ;
            }
            for(String s: names){
                MORFullIndividual ind=new MORFullIndividual(s,
                        ontoRef);
                ind.readSemantic();
                int number=ind.getLiteral(
                        Property).parseInteger();
                number++;
                ind.removeData(Property);
                ind.addData(Property,number);
                ind.writeSemantic();
            }

        }
        public List<String> getToBeForgottenSemantic(){return this.toBeForgottenSemantic;}
        public List<String> getToBeForgottenEpisodic(){return this.toBeForgottenEpisodic;}
        public List<String> getLowScoreSemantic(){return this.lowScoreSemantic;}
        public List<String> getLowScoreEpisodic(){return this.lowScoreEpisodic;}
        public List<String> getForgotSemantic(){return this.forgotSemantic;}
        public List<String> getForgotEpisodic(){return this.forgotEpisodic;}




    }
    public void objectPropertyValues(MORAxioms.ObjectSemantics objProp,String property,List<String> individuals){
        for (MORAxioms.ObjectSemantic obj : objProp) {
            if (obj.toString().contains(property)) {
                MORAxioms.Individuals ind = obj.getValues();
                for (OWLNamedIndividual i : ind) {
                    //add to the string the new score
                    //TODO change such that it depends on the onto ref and not on the string SCORE IRI ONTO
                    individuals.add(i.toStringID().substring(SCORE.SCORE_IRI_ONTO.length() + 1));
                }

            }
        }
    }
}
