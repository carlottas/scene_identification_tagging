package it.emarolab.scene_identification_tagging;

import it.emarolab.owloop.aMORDescriptor.utility.concept.MORFullConcept;
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
import it.emarolab.owloop.aMORDescriptor.MORAxioms;
import it.emarolab.owloop.aMORDescriptor.utility.individual.MORFullIndividual;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.util.List;
import java.util.ArrayList;

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
                    SemanticScore semanticScore = new SemanticScore(semantic.getName(), semantic.getSubClasses(), semantic.getSuperClasses(),semantic.getFirstSuperClass(),semantic.getIsFirstSuperCLassOf(),ontoRef,true);
                    //if retrieval
                    semanticScore.semanticRetrieval();
                }
                if(!episodic.getName().isEmpty()){
                    EpisodicScore episodicScore= new EpisodicScore(episodic.getName(),episodic.getNameSemanticItem(),ontoRef,true);
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
        private List<String> subClasses= new ArrayList<>();
        private List<String> superClasses= new ArrayList<>();
        private List<String> belongingIndividuals=new ArrayList<>();
        private List<String> isFirstSuperCLassOf=new ArrayList<>();
        MORFullIndividual scoreSemantic;
        MORFullIndividual totalScoreSemantic;
        MORFullIndividual totalScoreEpisodic;
        boolean addTime=true;
        private Long time = System.currentTimeMillis();
        OWLReferences ontoRef;

        /**
         * Constructor, in this constructor the semantic score is also initialized hence it has to be used only when the score must be
         * created also in the ontology itself
         * @param Name Name of the semanticScore Individual
         * @param SubClasses List of Name of the subClasses of the semanticScore Individual
         * @param SuperClasses List of the Name of the superClasses of the semantic Score Individual
         * @param firstSuperClass String containing the Name of the first super class of the semantic score individal
         * @param ontoRef Reference to the ontology to be manipulated
         * @param AddTime Boolean, if true also the time information are added
         */
        public SemanticScore(String Name, List<String> SubClasses,
                             List<String> SuperClasses,String firstSuperClass,List<String> isFirstSuperCLassOf, OWLReferences ontoRef,boolean AddTime) {
            //everytime this function is called it means that it needs to be initialized
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
            this.isFirstSuperCLassOf=isFirstSuperCLassOf;
            this.addTime=AddTime;
            semanticInitialization();
        }

        /**
         * Constructor, it has to be used when the score has only to be manipulate from the ontology
         * @param Name Name of the semantic Score individual
         * @param ontoRef Reference to the Ontology to be manipulated
         */
        public SemanticScore(String Name, OWLReferences ontoRef) {
            scoreSemantic = new MORFullIndividual(Name,
                    ontoRef);
            totalScoreSemantic = new MORFullIndividual(SCORE.SCORE_INDIVIDUAL_TOTAL_SEMANTIC,
                    ontoRef);
            totalScoreEpisodic = new MORFullIndividual(SCORE.SCORE_INDIVIDUAL_TOTAL_EPISODIC,
                    ontoRef);
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
         * function which initialize the semantic item
         */
        public void semanticInitialization() {
            // add the individual to the class
            ontoRef.synchronizeReasoner();
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
            if(addTime){
                scoreSemantic.addData(SCORE.SCORE_PROP_HAS_TIME,time);
            }
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
            for (String s:isFirstSuperCLassOf){
                MORFullIndividual ind= new MORFullIndividual(s, ontoRef);
                ind.readSemantic();
                ind.removeObject(SCORE.SCORE_OBJ_PROP_FIRST_SUPERCLASS);
                ind.addObject(SCORE.SCORE_OBJ_PROP_FIRST_SUPERCLASS,this.Name);
                ind.writeSemantic();
            }
            scoreSemantic.writeSemantic();
            scoreSemantic.saveOntology(SCORE.SCORE_FILE_PATH);
            //updating super class score
            System.out.println("updating super classes score...");
            updateSuperClassScore(superClasses, scoreComputed);
        }

        /**
         * function which modified the semantic item score if it has been retrieved
         */
        public void semanticRetrieval() {
            scoreSemantic.readSemantic();
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
        /**
         *
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
         *
         * @param ind individual of which the score must be computed
         * @return the score
         */
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

        /**
         * update the total semantic score  when a new item is added
         * @param scoreComputed :score of the semantic item added
         */
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

        /**
         * Update the total Semantic Score when the score of an Item is changed
         * @param oldScore of the semantic item modified
         * @param newScore of the semantic item modified
         */
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

        /**
         * Updating superclasses score when a score of a subclass has changed
         * @param classesNames names of the superclasses to be updated
         * @param scoreOld old score of the changed semantic item
         * @param scoreNew new score of the changed semantic item
         */
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

        /**
         * Update superclasses score when a new subclass has been added
         * @param setName name of the superclasses to be updated
         * @param score score of the semantic item to be added
         */
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

        public void deleteFromSuperClasses(){
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
                float newScore = computeScore(Sup);
                //update the semantic with the new score
                UpdateTotalSemanticScore(Sup.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(),newScore);
                //update the ontology
                Sup.removeData(SCORE.SCORE_PROP_HAS_SCORE);
                Sup.addData(SCORE.SCORE_PROP_HAS_SCORE, newScore);
                Sup.writeSemantic();

            }

        }
        /**
         * Function which deletates the information about an episodic item form the semantic item
         * @param s name of the episodic item
         * @param score value of its score
         */
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
            updateSuperClassScore(superClasses, scoreSemantic.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(), newScore);
            scoreSemantic.removeData(SCORE.SCORE_PROP_HAS_SCORE);
            scoreSemantic.addData(SCORE.SCORE_PROP_HAS_SCORE, newScore);
            scoreSemantic.writeSemantic();
        }
        /**
         * function which forgets the semantic score item
         */
        public void forgetItem() {
            ontoRef.synchronizeReasoner();
            //read the current ontology state
            scoreSemantic.readSemantic();
            //Remove its score from the total semantic score
            UpdateTotalSemanticScore(scoreSemantic.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(), 0);
            // if there is no super class
            if (firstSuperClass.equals(CLASS.SCENE)){
                //delete the belonging individuals
                for (String s : belongingIndividuals) {
                    //update the score of the first superClass
                    EpisodicScore ep = new EpisodicScore(s,ontoRef);
                    ep.forgetItem();
                }
            }
            //if there exist the first class --> Hyp it is unique
            else {
                //All its individual will belong to the first superclass
                //for all the individuals
                for (String i : belongingIndividuals) {
                    //declaration of the object
                    EpisodicScore episodicScore = new EpisodicScore(i, ontoRef);
                    episodicScore.changeSemanticItem(this.firstSuperClass);
                }
                UpdateFirstSuperClassScore();
            }
            //this is done even if there is no superclass
            //REMOVE THE SCORE AND THE CLASS TO THE OTHER CLASSES
            deleteFromSuperClasses();
            // the classes of which it was the first superclass now will have its first superclass as first superclass
            UpdateFirstSuperClass();
            //now that all the individual have been associated to the super class one can delete the individual
            ontoRef.synchronizeReasoner();
            ontoRef.removeIndividual(this.Name);
        }
        /**
         * update the fist super classes score once the element has been deleted
         */
        private void UpdateFirstSuperClassScore(){
            MORFullIndividual firstSup = new MORFullIndividual(this.firstSuperClass, ontoRef);
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
        }
        private void UpdateFirstSuperClass(){
            for(String s : isFirstSuperCLassOf){
                //declaration of the object
                MORFullIndividual subCl = new MORFullIndividual(s,
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
        /**
         * function which update the object list of belonging individuals
         */
        private void BelongingIndividual() {
            objectPropertyValues(scoreSemantic.getObjectSemantics(), SCORE.SCORE_OBJ_PROP_HAS_INDIVIDUAL, belongingIndividuals);

        }

        /**
         * function which update the object list of super classes
         */
        private void SuperClasses() {
            objectPropertyValues(scoreSemantic.getObjectSemantics(), SCORE.SCORE_OBJ_PROP_IS_SUB_CLASS_OF, superClasses);
        }

        /**
         * function which update the object list of subclasses
         */
        private void SubClasses(){
            objectPropertyValues(scoreSemantic.getObjectSemantics(),SCORE.SCORE_OBJ_PROP_IS_SUPER_CLASS_OF,subClasses);
        }

        /**
         * function which update the Object first super class name
         */
        private void FirstSuperClass(){
            List<String> firstSup= new ArrayList<>();
            objectPropertyValues(scoreSemantic.getObjectSemantics(),SCORE.SCORE_OBJ_PROP_FIRST_SUPERCLASS,firstSup);
            for (String i : firstSup){
                this.firstSuperClass= i;

            }
        }

        /**
         * function which update the Object first superClass NAME
         */
        private void IsFirstSuperClassOf(){
            objectPropertyValues(scoreSemantic.getObjectSemantics(),SCORE.SCORE_OBJ_PROP_IS_FIRST_SUPER_CLASS_OF,
                    this.isFirstSuperCLassOf);
        }
        /**
         * function which update all the object paramenters
         */
        public void updateAllRelations(){
            BelongingIndividual();
            SuperClasses();
            SubClasses();
            FirstSuperClass();
            IsFirstSuperClassOf();
        }

        /**
         *
         * @return the List of Sub Classes
         */
        public List<String> getSubClasses(){return this.subClasses;}

        /**
         *
         * @return the list of SuperClasses
         */
        public List<String> getSuperClasses(){return this.superClasses;}

        /**
         *
         * @return the list of Belonging Individuals
         */
        public List<String> getBelongingIndividuals(){return this.belongingIndividuals;}

        /**
         *
         * @return the String containing the name of the fist super class
         */
        public String getFirstSuperClass(){return this.firstSuperClass;}

        /**
         *
         * @return the Individual score Semantic
         */
        public MORFullIndividual getScoreSemantic(){return this.scoreSemantic;}

        /**
         *
         * @return the individual total Score Semantic
         */
        public MORFullIndividual getTotalScoreSemantic(){return this.totalScoreSemantic;}

        /**
         *
         * @return the individual total score Episodic
         */
        public MORFullIndividual getTotalScoreEpisodic(){return this.totalScoreEpisodic;}

        /**
         *
         * @return the string containing the name of the semantic score individual
         */
        public String getName(){return this.Name;}


    }
    public class EpisodicScore{
    private String Name;
    private SemanticScore SemanticItem;
    MORFullIndividual scoreEpisodic;
    MORFullIndividual totalScoreEpisodic;
    boolean addTime=true;
    private Long time = System.currentTimeMillis();
    OWLReferences ontoRef;

    /**
     * Constructor , when this constructor is used, it is assumed that the item does not
     * exists in the ontology itself hence it is initialized
     * @param Name name of the episodic item score
     * @param SemanticName name of the semanti item to which this item belongs
     * @param ontoRef reference to the ontology to manipulate
     */
    public EpisodicScore(String Name, String SemanticName,OWLReferences ontoRef,boolean addTime) {
        this.Name = Name;
        scoreEpisodic = new MORFullIndividual(Name,
                ontoRef);
        totalScoreEpisodic = new MORFullIndividual(SCORE.SCORE_INDIVIDUAL_TOTAL_EPISODIC,
                ontoRef);
        SemanticItem = new SemanticScore(SemanticName,ontoRef);
        this.ontoRef=ontoRef;
        this.addTime=addTime;
        episodicInitialization();
    }
    /**
     * Constructo,When this constructor is used it is assumed that the episodic score item already
     * exists in the ontology
     * @param Name Name of the episodic Item
     * @param ontoRef Reference to the ontology to be manipulated
     */
    public EpisodicScore(String Name, OWLReferences ontoRef){
        this.Name=Name;
        this.ontoRef=ontoRef;
        scoreEpisodic = new MORFullIndividual(Name,ontoRef);
        totalScoreEpisodic= new MORFullIndividual(SCORE.SCORE_INDIVIDUAL_TOTAL_EPISODIC,ontoRef);
        UpdateSemanticItem();

    }

    /**
     * Function which initialize the episodic score item
     */
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
        if(addTime){
            scoreEpisodic.addData(SCORE.SCORE_PROP_HAS_TIME,time);
        }
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
    /**
     * function which update the episodic score if the episodic item has been retrieved
     */
    public void episodicRetrieval(){
        scoreEpisodic.readSemantic();
        int numberEpisodicRetrieval=scoreEpisodic.getLiteral(
                SCORE.SCORE_PROP_NUMBER_EPISODIC_RETRIEVAL).parseInteger();
        numberEpisodicRetrieval++;
        float newScore=computeScore(scoreEpisodic.getLiteral(SCORE.SCORE_PROP_NUMBER_SEMANTIC_RETRIEVAL).parseInteger(),
                numberEpisodicRetrieval);
        updateTotalEpisodicScore(scoreEpisodic.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(),
                newScore);

        updateSemanticFromIndividual(scoreEpisodic.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(),newScore);
        scoreEpisodic.removeData(SCORE.SCORE_PROP_NUMBER_EPISODIC_RETRIEVAL);
        scoreEpisodic.addData(SCORE.SCORE_PROP_NUMBER_EPISODIC_RETRIEVAL,numberEpisodicRetrieval);
        scoreEpisodic.removeData(SCORE.SCORE_PROP_HAS_SCORE);
        scoreEpisodic.addData(SCORE.SCORE_PROP_HAS_SCORE,newScore);
        scoreEpisodic.writeSemantic();
    }
    /**
     * fpnction which forget the episodic item hence delete it from the ontology
     */
    public void forgetItem() {
        updateTotalEpisodicScore(scoreEpisodic.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(), 0);
        SemanticItem.deleteEpisodicItem(this.Name,scoreEpisodic.getLiteral(SCORE.SCORE_PROP_HAS_VALUE).parseFloat());
        ontoRef.removeIndividual(this.Name);
        ontoRef.synchronizeReasoner();

    }

    /**
     * function which update the episodic item score if the class to which
     * it belong has been retrieved
     */
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
        //update the score of the classes
        updateSemanticFromIndividual(scoreEpisodic.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(), newScore);
        scoreEpisodic.removeData(SCORE.SCORE_PROP_NUMBER_SEMANTIC_RETRIEVAL);
        scoreEpisodic.addData(SCORE.SCORE_PROP_NUMBER_SEMANTIC_RETRIEVAL,semanticRetrieval);
        scoreEpisodic.removeData(SCORE.SCORE_PROP_HAS_SCORE);
        scoreEpisodic.addData(SCORE.SCORE_PROP_HAS_SCORE,newScore);
        scoreEpisodic.writeSemantic();
    }


    /**
     * function which update the SemanticItem variable of the class
     */
    private void UpdateSemanticItem(){
        List<String> Names = new ArrayList<>();
        String Name="";
        scoreEpisodic.readSemantic();
        ontoRef.synchronizeReasoner();
        objectPropertyValues(scoreEpisodic.getObjectSemantics(),SCORE.SCORE_OBJ_PROP_IS_INDIVIDUAL_OF,Names);
        for(String s: Names){
            Name = s;
        }
        this.SemanticItem= new SemanticScore(Name,ontoRef);
    }

    /**
     * Function which compute the episodic score
     * @param semantic_retrieval number of semantic retrieval
     * @param episodic_retrieval number of episodic retrieval
     * @return episodic score
     */
    private float computeScore(int semantic_retrieval,
                               int episodic_retrieval) {
        return ((float) (SCORE.SCORE_EPISODIC_WEIGHT_1 * semantic_retrieval +
                SCORE.SCORE_EPISODIC_WEIGHT_2 * episodic_retrieval));


    }

    /**
     * function which updated the total episodic score if a new episodic item has been created
     * @param score value of the score of the new episodic item
     */
    public void updateTotalEpisodicScore(float score){
        totalScoreEpisodic.readSemantic();
        float total=totalScoreEpisodic.getLiteral(SCORE.SCORE_PROP_HAS_VALUE).parseFloat();
        total += score;
        totalScoreEpisodic.removeData(SCORE.SCORE_PROP_HAS_VALUE);
        totalScoreEpisodic.addData(SCORE.SCORE_PROP_HAS_VALUE,total);
        totalScoreEpisodic.writeSemantic();
    }

    /**
     * function which updated the total episodic score if the episodic score of
     * an item has been changed
     * @param oldScore old score of the episodic item
     * @param newScore new score of the episodic item
     */
    public void updateTotalEpisodicScore(float oldScore,float newScore){
        totalScoreEpisodic.readSemantic();
        float total=totalScoreEpisodic.getLiteral(SCORE.SCORE_PROP_HAS_VALUE).parseFloat();
        total-=oldScore;
        total+=newScore;
        totalScoreEpisodic.removeData(SCORE.SCORE_PROP_HAS_VALUE);
        totalScoreEpisodic.addData(SCORE.SCORE_PROP_HAS_VALUE,total);
        totalScoreEpisodic.writeSemantic();

    }

    /**
     * Function which updates the score of the semantic item to which
     * a new episodic score belong
     * @param episodicName name of the episodic item
     * @param Score score of the item
     */
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

    /**
     * Function which updates the score of the semantic item to which
     * an episodic score belong when the latter has modified its score
     * @param oldScore old score of the episodic item
     * @param newScore new score of the episodic item
     */
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

    /**
     * Function which update the semantic score in case
     * the episodic individual have changed values
     * @param s name of the semantic item
     * @param oldScore old score of the episodic item
     * @param newScore new score of the episodic item
     */
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
    /**
     * change the Semantic Item which the episodic item belongs to
     *
     * @param newSemanticItem new semantic item
     */
    public void changeSemanticItem(String newSemanticItem){
        scoreEpisodic.readSemantic();
        scoreEpisodic.removeData(SCORE.SCORE_OBJ_PROP_IS_INDIVIDUAL_OF);
        scoreEpisodic.addData(SCORE.SCORE_OBJ_PROP_IS_INDIVIDUAL_OF,newSemanticItem);
        scoreEpisodic.writeSemantic();
        SemanticItem= new SemanticScore(newSemanticItem,ontoRef);
    }
}
public class Forgetting {
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
