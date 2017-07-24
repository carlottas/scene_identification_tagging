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
import  it.emarolab.scene_identification_tagging.ROSSemanticInterface;

import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

public class ScoreService extends  ROSSemanticInterface.ROSSemanticServer<ScoreInterfaceRequest, ScoreInterfaceResponse>
        implements SITBase {
    private static final String SERVICE_NAME = "ScoreService";

    public boolean initParam(ConnectedNode node) {

        // stat the service
        node.newServiceServer(
                getServerName(), // set service name
                ScoreInterface._TYPE, // set ROS service message
                getService(node) // set ROS service response
        );
        loadSemantics(SCORE.SCORE_ONTO_NAME,SCORE.SCORE_FILE_PATH,SCORE.SCORE_IRI_ONTO);
        return true;
    }

    public String getServerName() {
        return SERVICE_NAME;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(getServerName());
    }



    /**
     * @param node the bridge to the standard ROS service
     * @return the object that defines the computation to be performed during service call.
     */

    public ServiceResponseBuilder<ScoreInterfaceRequest, ScoreInterfaceResponse> getService(final ConnectedNode node) {
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

                EpisodicScoreItem episodic = request.getEpisodic();
                SemanticScoreItem semantic = request.getSemantic();
                int decision = request.getDecision();
                OWLReferences ontoRef = getOntology();
                // suppress aMOR log
                it.emarolab.amor.owlDebugger.Logger.setPrintOnConsole(false);
                //MEMORIZATION
                if (decision == 1) {
                    if (!semantic.getName().isEmpty()) {
                        SemanticScore semanticScore = new SemanticScore(semantic.getName(), semantic.getSubClasses(), semantic.getSuperClasses(), semantic.getFirstSuperClass(), semantic.getIsFirstSuperCLassOf(), ontoRef, true);

                    }
                    if (!episodic.getName().isEmpty()) {
                        EpisodicScore episodicScore = new EpisodicScore(episodic.getName(), episodic.getNameSemanticItem(), ontoRef, true);


                    }
                }
                //RETRIEVAL
                else if (decision == 2 || decision==4) {
                    if (!request.getSemanticRetrieval().isEmpty()) {
                        for (String s : request.getSemanticRetrieval()) {
                            if (!s.equals("owlNothing")) {
                                SemanticScore score = new SemanticScore(s, ontoRef);
                                score.semanticRetrieval();
                            }

                        }
                    }
                    if (!request.getEpisodicRetrieval().isEmpty()) {
                        for (String s : request.getEpisodicRetrieval()) {
                            EpisodicScore score = new EpisodicScore(s, ontoRef);
                            score.episodicRetrieval();
                        }
                    }
                    for(String s : request.getUserNoForget()){
                        resetCounter(s,ontoRef);
                        changeUserNoForget(s,ontoRef,true);
                    }
                    for (String s : request.getResetCounter()){
                        resetCounter(s,ontoRef);
                    }
                    //the forgetting counter is done everytime the retrieval is finished
                    Forgetting forgetting = new Forgetting(ontoRef);
                    forgetting.updateTimes();
                    forgetting.updateLists();
                    //filling the response
                    forgetting.deleteEpisodic();
                    forgetting.deleteSemantic();
                    response.setDeleteEpisodic(forgetting.getForgotEpisodic());
                    response.setDeleteSemantic(forgetting.getForgotSemantic());
                    response.setPutForgotEpisodic(forgetting.getToBeForgottenEpisodic());
                    response.setPutForgotSemantic(forgetting.getToBeForgottenSemantic());
                    ontoRef.saveOntology(SCORE.SCORE_FILE_PATH);
                }
                //FORGETTING
                else if (decision == 3) {
                    int forgettingDecision = request.getDecisionForgetting();
                    if (forgettingDecision == 1) {

                        ScoreCounterArray toBeForgettingSemantic= new ScoreCounterArray();
                        ScoreCounterArray toBeForgettingEpisodic= new ScoreCounterArray();
                        ScoreCounterArray forgotEpisodic = new ScoreCounterArray();
                        ScoreCounterArray forgotSemantic= new ScoreCounterArray();
                        ScoreCounterArray lowScoreSemantic= new ScoreCounterArray();
                        ScoreCounterArray lowScoreEpisodic= new ScoreCounterArray();

                        System.out.println("declaration of forgetting\n");
                        System.out.println("onto ref"+ ontoRef);
                        Forgetting forgetting = new Forgetting(ontoRef);

                        //forgetting.updateLists();
                        for(String s : forgetting.getToBeForgottenEpisodic()){
                            toBeForgettingEpisodic.add(createToBeForgettingItem(s,ontoRef,SCORE.SCORE_PROP_TIMES_TO_BE_FORGOTTEN));
                        }
                        for(String s : forgetting.getToBeForgottenSemantic()){
                            toBeForgettingSemantic.add(createToBeForgettingItem(s,ontoRef,SCORE.SCORE_PROP_TIMES_TO_BE_FORGOTTEN));
                        }
                        for(String s : forgetting.getForgotEpisodic()){
                            forgotEpisodic.add(createToBeForgettingItem(s,ontoRef,SCORE.SCORE_PROP_TIMES_FORGOTTEN));
                        }
                        for(String s : forgetting.getForgotSemantic()){
                            forgotSemantic.add(createToBeForgettingItem(s,ontoRef,SCORE.SCORE_PROP_TIMES_FORGOTTEN));
                        }
                        for(String s : forgetting.getLowScoreEpisodic()){
                            lowScoreEpisodic.add(createToBeForgettingItem(s,ontoRef,SCORE.SCORE_PROP_TIMES_LOW_SCORE));
                        }
                        for(String s : forgetting.getLowScoreSemantic()){
                            lowScoreSemantic.add(createToBeForgettingItem(s,ontoRef,SCORE.SCORE_PROP_TIMES_LOW_SCORE));
                        }
                        response.setToBeForgottenEpisodic(toBeForgettingEpisodic.mapInROSMsg(node));
                        response.setToBeForgottenSemantic(toBeForgettingSemantic.mapInROSMsg(node));
                        response.setForgotEpisodic(forgotEpisodic.mapInROSMsg(node));
                        response.setForgotSemantic(forgotSemantic.mapInROSMsg(node));
                        response.setLowScoreEpisodic(lowScoreEpisodic.mapInROSMsg(node));
                        response.setLowScoreSemantic(lowScoreSemantic.mapInROSMsg(node));

                    }
                    else if (forgettingDecision ==2){
                        for(String s : request.getEpisodicForgot()){
                            //forgot
                        }
                        for(String s : request.getSemanticForgot()){
                            //forgot
                        }


                    }
                    else if (forgettingDecision==3){
                        //saving the item
                        for(String s : request.getUserPutNoForget()){
                            resetCounter(s,ontoRef);
                            changeUserNoForget(s,ontoRef,true);
                        }

                    }
                    else if (forgettingDecision == 4){
                        //removing the save item
                        for (String s : request.getUserRemoveNoForget()){
                            changeUserNoForget(s,ontoRef,false);

                        }
                    }
                }
            }


        };


    }


    private class SemanticScore {
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
         * Constructor, in this constructor the semantic score is also initialized hence it has to be used only when the score must be
         * created also in the ontology itself
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
            //everytime this function is called it means that it needs to be initialized
            scoreSemantic = new MORFullIndividual(Name,
                    ontoRef);
            totalScoreSemantic = new MORFullIndividual(SCORE.SCORE_INDIVIDUAL_TOTAL_SEMANTIC,
                    ontoRef);
            totalScoreEpisodic = new MORFullIndividual(SCORE.SCORE_INDIVIDUAL_TOTAL_EPISODIC,
                    ontoRef);
            this.ontoRef = ontoRef;
            this.Name = Name;
            SubClasses.remove("owlNothing");
            System.out.println("Subclaasses after removal of owl nothing inside the score\n");
            System.out.println(SubClasses.size() + " " + SubClasses);
            this.subClasses = SubClasses;
            SuperClasses.remove(CLASS.SCENE);
            this.superClasses = SuperClasses;
            this.firstSuperClass = firstSuperClass;
            this.isFirstSuperCLassOf = isFirstSuperCLassOf;
            this.addTime = AddTime;
            semanticInitialization();
        }

        /**
         * Constructor, it has to be used when the score has only to be manipulate from the ontology
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
            if (addTime) {
                scoreSemantic.addData(SCORE.SCORE_PROP_HAS_TIME, time);
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
            for (String s : this.firstSuperClass) {
                scoreSemantic.addObject(SCORE.SCORE_OBJ_PROP_FIRST_SUPERCLASS, s);
            }
            for (String s : this.isFirstSuperCLassOf) {
                MORFullIndividual ind = new MORFullIndividual(s, ontoRef);
                ind.readSemantic();
                ind.removeObject(SCORE.SCORE_OBJ_PROP_FIRST_SUPERCLASS);
                ind.addObject(SCORE.SCORE_OBJ_PROP_FIRST_SUPERCLASS, this.Name);
                ind.writeSemantic();
            }
            scoreSemantic.writeSemantic();
            ontoRef.synchronizeReasoner();
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
            for (String s : belongingIndividuals) {
                EpisodicScore ep = new EpisodicScore(s, ontoRef);
                ep.episodicSemanticRetrieval();
            }
            scoreSemantic.readSemantic();
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
            scoreSemantic.saveOntology(SCORE.SCORE_FILE_PATH);
            //find the individual that have been retrieved with the
            // semantic item
            //updating the belonging individual score

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
            ind.readSemantic();
            System.out.println("semantic item in the compute score" + ind.toString());
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
            // ind.removeData(SCORE.SCORE_PROP_SCORE_SUB_CLASSES);
            //ind.removeData(SCORE.SCORE_PROP_SCORE_BELONGING_INDIVIDUAL);
            // ind.removeData(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL);
            //ind.addData(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL,scoreIndividual);
            //ind.writeSemantic();
            //ind.saveOntology(SCORE.SCORE_FILE_PATH);
            return ((float) (SCORE.SCORE_SEMANTIC_WEIGHT_1 * numberBelongingIndividual +
                    SCORE.SCORE_SEMANTIC_WEIGHT_2 * scoreIndividual +
                    SCORE.SCORE_SEMANTIC_WEIGHT_3 * numberSubClasses +
                    SCORE.SCORE_SEMANTIC_WEIGHT_4 * scoreSubClasses +
                    SCORE.SCORE_SEMANTIC_WEIGHT_5 * retrieval));
        }

        private float computeScore(int numberBelongingIndividual, int numberSubClasses, float sumScoreBelongingIndividua,
                                   float sumScoreSubClasses, int retrieval) {
            float scoreSubClasses;
            float scoreIndividual;
            //if the total semantic is equal to 0
            //TODO alla fine hai deciso di dividerlo per il numero di persone ecc perchè cosi e ceoerente se no in qualunque caso
            //sarebbe stato uno score sbagliato che avresti dovuto computare ogni volta che cambiava qualunque cosa
            // l'unica soluzione potrebbe essere computare lo score direttamente antraverso una SWRL rule
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
         *
         * @param oldScore of the semantic item modified
         * @param newScore of the semantic item modified
         */
        public void UpdateTotalSemanticScore(float oldScore, float newScore) {
            System.out.println("IMPORTANTE SEI DENTRO IL TOTAL SEMANTIC SCORE");
            System.out.println(totalScoreSemantic.toString());
            System.out.println("oldscore ind " + oldScore);
            System.out.println("new score ind " + newScore);
            //read the current state of total semantic item
            totalScoreSemantic.readSemantic();
            //reading the value of hasValue dataproperty
            float total = totalScoreSemantic.getLiteral(SCORE.SCORE_PROP_HAS_VALUE).parseFloat();
            //updating the value
            System.out.println("old total " + total);
            total -= oldScore;
            total += newScore;
            System.out.println("new total " + total);
            //updating the data property with the new value just computed
            totalScoreSemantic.removeData(SCORE.SCORE_PROP_HAS_VALUE);
            System.out.println("adding the data");
            totalScoreSemantic.addData(SCORE.SCORE_PROP_HAS_VALUE, total);
            totalScoreSemantic.writeSemantic();
            System.out.println("total after all changes \n" + totalScoreSemantic.toString());
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
            //for all the string
            for (String name : classesNames) {
                if (!name.equals(CLASS.SCENE)) {
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
            /*
            //TODO no sense
            if(setName.size()==1 && setName.contains(CLASS.SPHERE)){
                return;
            }
            */
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

        public void deleteFromSuperClasses() {
            for (String sup : this.superClasses) {
                //declare the object
                MORFullIndividual Sup = new MORFullIndividual(sup, ontoRef);
                //read the current ontology state
                Sup.readSemantic();
                //remove the objcet actribut is superclass of
                Sup.removeObject(SCORE.SCORE_OBJ_PROP_IS_SUPER_CLASS_OF, this.Name);
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
                UpdateTotalSemanticScore(Sup.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(), newScore);
                //update the ontology
                Sup.removeData(SCORE.SCORE_PROP_HAS_SCORE);
                Sup.addData(SCORE.SCORE_PROP_HAS_SCORE, newScore);
                Sup.writeSemantic();

            }

        }

        /**
         * Function which deletates the information about an episodic item form the semantic item
         *
         * @param s     name of the episodic item
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
        public List<String> forgetItem() {
            ontoRef.synchronizeReasoner();
            //read the current ontology state
            scoreSemantic.readSemantic();
            //Remove its score from the total semantic score
            UpdateTotalSemanticScore(scoreSemantic.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(), 0);
            // if there is no super class
            //if (firstSuperClass.size() == 1 && firstSuperClass.get(0).equals(CLASS.SCENE)) {
                //delete the belonging individuals
                for (String s : belongingIndividuals) {
                    //update the score of the first superClass
                    EpisodicScore ep = new EpisodicScore(s, ontoRef);
                    ep.forgetItem();
                }
                ontoRef.removeIndividual(this.Name);
                return  belongingIndividuals;
            //}
            /*
            //if there exist the first class --> Hyp it is unique
            else {
                //All its individual will belong to the first superclass
                //for all the individuals
                for (String i : belongingIndividuals) {
                    //declaration of the object
                    EpisodicScore episodicScore = new EpisodicScore(i, ontoRef);
                    episodicScore.changeSemanticItem(firstSuperClass.get(0));
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
        */
        }

        /**
         * update the fist super classes score once the element has been deleted
         */
        private void UpdateFirstSuperClassScore() {
            MORFullIndividual firstSup = new MORFullIndividual(firstSuperClass.get(0), ontoRef);
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
            updateSuperClassScore(superCl, firstSup.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(), newScore);
            //update the ontology
            firstSup.removeData(SCORE.SCORE_PROP_HAS_SCORE);
            firstSup.addData(SCORE.SCORE_PROP_HAS_SCORE, newScore);
            firstSup.writeSemantic();
            firstSup.saveOntology(SCORE.SCORE_FILE_PATH);
        }

        private void UpdateFirstSuperClass() {
            //declaration of the object
            for (String s : this.isFirstSuperCLassOf) {
                MORFullIndividual subCl = new MORFullIndividual(s,
                        ontoRef);
                //read the current ontology state
                subCl.readSemantic();
                //update the first superclass object property
                subCl.removeObject(SCORE.SCORE_OBJ_PROP_FIRST_SUPERCLASS, this.Name);
                subCl.addObject(SCORE.SCORE_OBJ_PROP_FIRST_SUPERCLASS, firstSuperClass.get(0));
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
            superClasses.remove(CLASS.SCENE);
        }

        /**
         * function which update the object list of subclasses
         */
        private void SubClasses() {
            objectPropertyValues(scoreSemantic.getObjectSemantics(), SCORE.SCORE_OBJ_PROP_IS_SUPER_CLASS_OF, subClasses);
        }

        /**
         * function which update the Object first super class name
         */
        private void FirstSuperClass() {

            objectPropertyValues(scoreSemantic.getObjectSemantics(), SCORE.SCORE_OBJ_PROP_FIRST_SUPERCLASS, this.firstSuperClass);
        }

        /**
         * function which update the Object first superClass NAME
         */
        private void IsFirstSuperClassOf() {

            objectPropertyValues(scoreSemantic.getObjectSemantics(), SCORE.SCORE_OBJ_PROP_IS_FIRST_SUPER_CLASS_OF,
                    isFirstSuperCLassOf);


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

    public class EpisodicScore {
        private String Name;
        private SemanticScore SemanticItem;
        MORFullIndividual scoreEpisodic;
        MORFullIndividual totalScoreEpisodic;
        boolean addTime = true;
        private Long time = System.currentTimeMillis();
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
            this.ontoRef = ontoRef;
            this.addTime = addTime;
            episodicInitialization();
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
            this.ontoRef = ontoRef;
            scoreEpisodic = new MORFullIndividual(Name, ontoRef);
            totalScoreEpisodic = new MORFullIndividual(SCORE.SCORE_INDIVIDUAL_TOTAL_EPISODIC, ontoRef);
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
            if (addTime) {
                scoreEpisodic.addData(SCORE.SCORE_PROP_HAS_TIME, time);
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
            ontoRef.synchronizeReasoner();
            scoreEpisodic.saveOntology(SCORE.SCORE_FILE_PATH);
        }

        /**
         * function which update the episodic score if the episodic item has been retrieved
         */
        public void episodicRetrieval() {
            scoreEpisodic.readSemantic();
            int numberEpisodicRetrieval = scoreEpisodic.getLiteral(
                    SCORE.SCORE_PROP_NUMBER_EPISODIC_RETRIEVAL).parseInteger();
            numberEpisodicRetrieval++;
            float newScore = computeScore(scoreEpisodic.getLiteral(SCORE.SCORE_PROP_NUMBER_SEMANTIC_RETRIEVAL).parseInteger(),
                    numberEpisodicRetrieval);
            updateTotalEpisodicScore(scoreEpisodic.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(),
                    newScore);
            System.out.println("old score individual episodic " + scoreEpisodic.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat());
            System.out.println("new score individual episodic" + newScore);
            updateSemanticFromIndividual(scoreEpisodic.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(), newScore);
            scoreEpisodic.removeData(SCORE.SCORE_PROP_NUMBER_EPISODIC_RETRIEVAL);
            scoreEpisodic.addData(SCORE.SCORE_PROP_NUMBER_EPISODIC_RETRIEVAL, numberEpisodicRetrieval);
            scoreEpisodic.removeData(SCORE.SCORE_PROP_HAS_SCORE);
            scoreEpisodic.addData(SCORE.SCORE_PROP_HAS_SCORE, newScore);
            scoreEpisodic.writeSemantic();
            scoreEpisodic.saveOntology(SCORE.SCORE_FILE_PATH);
        }

        /**
         * fpnction which forget the episodic item hence delete it from the ontology
         */
        public void forgetItem() {
            updateTotalEpisodicScore(scoreEpisodic.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(), 0);
            SemanticItem.deleteEpisodicItem(this.Name, scoreEpisodic.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat());
            ontoRef.removeIndividual(this.Name);
            ontoRef.synchronizeReasoner();

        }

        /**
         * function which update the episodic item score if the class to which
         * it belong has been retrieved
         */
        public void episodicSemanticRetrieval() {
            scoreEpisodic.readSemantic();
            //changing the number of semantic retrieval
            int semanticRetrieval = scoreEpisodic.getLiteral(SCORE.SCORE_PROP_NUMBER_SEMANTIC_RETRIEVAL).parseInteger();
            semanticRetrieval++;
            //compute the new score
            float newScore = computeScore(
                    semanticRetrieval, scoreEpisodic.getLiteral(SCORE.SCORE_PROP_NUMBER_EPISODIC_RETRIEVAL).parseInteger());

            //update the total semantic score
            updateTotalEpisodicScore(scoreEpisodic.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(), newScore);
            //update the score of the classes
            updateSemanticFromIndividual(scoreEpisodic.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(), newScore);
            scoreEpisodic.removeData(SCORE.SCORE_PROP_NUMBER_SEMANTIC_RETRIEVAL);
            scoreEpisodic.addData(SCORE.SCORE_PROP_NUMBER_SEMANTIC_RETRIEVAL, semanticRetrieval);
            scoreEpisodic.removeData(SCORE.SCORE_PROP_HAS_SCORE);
            scoreEpisodic.addData(SCORE.SCORE_PROP_HAS_SCORE, newScore);
            scoreEpisodic.writeSemantic();
            scoreEpisodic.saveOntology(SCORE.SCORE_FILE_PATH);
        }


        /**
         * function which update the SemanticItem variable of the class
         */
        private void UpdateSemanticItem() {
            List<String> Names = new ArrayList<>();
            String Name = "";
            scoreEpisodic.readSemantic();
            ontoRef.synchronizeReasoner();
            objectPropertyValues(scoreEpisodic.getObjectSemantics(), SCORE.SCORE_OBJ_PROP_IS_INDIVIDUAL_OF, Names);
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
                                   int episodic_retrieval) {
            return ((float) (SCORE.SCORE_EPISODIC_WEIGHT_1 * semantic_retrieval +
                    SCORE.SCORE_EPISODIC_WEIGHT_2 * episodic_retrieval));


        }

        /**
         * function which updated the total episodic score if a new episodic item has been created
         *
         * @param score value of the score of the new episodic item
         */
        public void updateTotalEpisodicScore(float score) {
            totalScoreEpisodic.readSemantic();
            float total = totalScoreEpisodic.getLiteral(SCORE.SCORE_PROP_HAS_VALUE).parseFloat();
            total += score;
            totalScoreEpisodic.removeData(SCORE.SCORE_PROP_HAS_VALUE);
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

            totalScoreEpisodic.readSemantic();
            float total = totalScoreEpisodic.getLiteral(SCORE.SCORE_PROP_HAS_VALUE).parseFloat();
            total -= oldScore;
            total += newScore;
            totalScoreEpisodic.removeData(SCORE.SCORE_PROP_HAS_VALUE);
            totalScoreEpisodic.addData(SCORE.SCORE_PROP_HAS_VALUE, total);
            totalScoreEpisodic.writeSemantic();

        }

        /**
         * Function which updates the score of the semantic item to which
         * a new episodic score belong
         *
         * @param episodicName name of the episodic item
         * @param Score        score of the item
         */
        public void updateSemanticFromIndividual(String episodicName, float Score) {
            MORFullIndividual semanticIndividual = SemanticItem.getScoreSemantic();
            semanticIndividual.readSemantic();
            float scoreBelongingIndividual = semanticIndividual.getLiteral(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL).parseFloat();
            scoreBelongingIndividual += Score;
            semanticIndividual.removeData(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL);
            semanticIndividual.addData(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL, scoreBelongingIndividual);
            int numberBelongingIndividual = semanticIndividual.getLiteral(SCORE.SCORE_PROP_NUMBER_BELONGING_INDIVIDUAL).parseInteger();
            numberBelongingIndividual++;
            semanticIndividual.removeData(SCORE.SCORE_PROP_NUMBER_BELONGING_INDIVIDUAL);
            semanticIndividual.addData(SCORE.SCORE_PROP_NUMBER_BELONGING_INDIVIDUAL, numberBelongingIndividual);
            semanticIndividual.writeSemantic();
            float newScoreSemantic = SemanticItem.computeScore(semanticIndividual);
            SemanticItem.UpdateTotalSemanticScore(semanticIndividual.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(), newScoreSemantic);
            List<String> classes = new ArrayList<>();
            objectPropertyValues(semanticIndividual.getObjectSemantics(), SCORE.SCORE_OBJ_PROP_IS_SUB_CLASS_OF, classes);
            SemanticItem.updateSuperClassScore(classes,
                    semanticIndividual.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(),
                    newScoreSemantic);
            semanticIndividual.removeData(SCORE.SCORE_PROP_HAS_SCORE);
            semanticIndividual.addData(SCORE.SCORE_PROP_HAS_SCORE, newScoreSemantic);
            semanticIndividual.addObject(SCORE.SCORE_OBJ_PROP_HAS_INDIVIDUAL, episodicName);
            semanticIndividual.writeSemantic();
        }

        /**
         * Function which updates the score of the semantic item to which
         * an episodic score belong when the latter has modified its score
         *
         * @param oldScore old score of the episodic item
         * @param newScore new score of the episodic item
         */
        public void updateSemanticFromIndividual(float oldScore, float newScore) {
            System.out.println("IMPORTANTE dentro update semantic from individual");
            MORFullIndividual semanticIndividual = SemanticItem.getScoreSemantic();
            semanticIndividual.readSemantic();
            System.out.println("semantic item ");
            System.out.println(semanticIndividual.toString());
            float scoreBelongingIndividual = semanticIndividual.getLiteral(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL).parseFloat();
            System.out.println("IMPORTANTE \n\n\n\n\n");
            System.out.println("before changing the score " + scoreBelongingIndividual);
            scoreBelongingIndividual -= oldScore;
            scoreBelongingIndividual += newScore;
            System.out.println("after changing the score " + scoreBelongingIndividual);
            semanticIndividual.removeData(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL);
            semanticIndividual.addData(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL, scoreBelongingIndividual);
            semanticIndividual.writeSemantic();
            semanticIndividual.saveOntology(SCORE.SCORE_FILE_PATH);
            System.out.println("Semantic Item before calling the compute score\n" + semanticIndividual.toString());
            float newScoreSemantic = SemanticItem.computeScore(semanticIndividual.getLiteral(SCORE.SCORE_PROP_NUMBER_BELONGING_INDIVIDUAL).parseInteger(),
                    semanticIndividual.getLiteral(SCORE.SCORE_PROP_NUMBER_SUB_CLASSES).parseInteger(),
                    semanticIndividual.getLiteral(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL).parseFloat(),
                    semanticIndividual.getLiteral(SCORE.SCORE_PROP_SCORE_SUM_SUB_CLASSES).parseFloat(),
                    semanticIndividual.getLiteral(SCORE.SCORE_PROP_NUMBER_RETRIEVAL).parseInteger());
            System.out.println("NewScore " + newScoreSemantic);

            SemanticItem.UpdateTotalSemanticScore(semanticIndividual.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(), newScoreSemantic);
            List<String> classes = new ArrayList<>();
            objectPropertyValues(semanticIndividual.getObjectSemantics(), SCORE.SCORE_OBJ_PROP_IS_SUB_CLASS_OF, classes);
            SemanticItem.updateSuperClassScore(classes, semanticIndividual.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(),
                    newScoreSemantic);
            semanticIndividual.removeData(SCORE.SCORE_PROP_HAS_SCORE);
            semanticIndividual.addData(SCORE.SCORE_PROP_HAS_SCORE, newScoreSemantic);
            semanticIndividual.writeSemantic();
            semanticIndividual.saveOntology(SCORE.SCORE_FILE_PATH);
        }

        /**
         * Function which update the semantic score in case
         * the episodic individual have changed values
         *
         * @param s        name of the semantic item
         * @param oldScore old score of the episodic item
         * @param newScore new score of the episodic item
         */
        public void updateSemanticFromIndividual(String s, float oldScore, float newScore) {
            MORFullIndividual semanticIndividual = new MORFullIndividual(s, ontoRef);
            semanticIndividual.readSemantic();
            float scoreBelongingIndividual = semanticIndividual.getLiteral(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL).parseFloat();
            scoreBelongingIndividual -= oldScore;
            scoreBelongingIndividual += newScore;
            semanticIndividual.removeData(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL);
            semanticIndividual.addData(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL, scoreBelongingIndividual);
            semanticIndividual.writeSemantic();
            float newScoreSemantic = SemanticItem.computeScore(semanticIndividual);
            SemanticItem.UpdateTotalSemanticScore(semanticIndividual.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(), newScoreSemantic);
            List<String> classes = new ArrayList<>();
            objectPropertyValues(semanticIndividual.getObjectSemantics(), SCORE.SCORE_OBJ_PROP_IS_SUB_CLASS_OF, classes);
            SemanticItem.updateSuperClassScore(classes, semanticIndividual.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat(),
                    newScoreSemantic);
            semanticIndividual.removeData(SCORE.SCORE_PROP_HAS_SCORE);
            semanticIndividual.addData(SCORE.SCORE_PROP_HAS_SCORE, newScoreSemantic);
            semanticIndividual.writeSemantic();
        }

        /**
         * change the Semantic Item which the episodic item belongs to
         *
         * @param newSemanticItem new semantic item
         */
        public void changeSemanticItem(String newSemanticItem) {
            scoreEpisodic.readSemantic();
            scoreEpisodic.removeData(SCORE.SCORE_OBJ_PROP_IS_INDIVIDUAL_OF);
            scoreEpisodic.addData(SCORE.SCORE_OBJ_PROP_IS_INDIVIDUAL_OF, newSemanticItem);
            scoreEpisodic.writeSemantic();
            SemanticItem = new SemanticScore(newSemanticItem, ontoRef);
        }
    }

    public class Forgetting {
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
            toBeForgottenEpisodic= new ArrayList<>();
            toBeForgottenSemantic= new ArrayList<>();
            lowScoreSemantic= new ArrayList<>();
            lowScoreEpisodic= new ArrayList<>();
            forgotSemantic= new ArrayList<>();
            forgotEpisodic= new ArrayList<>(); 
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
                SemanticScore score = new SemanticScore(s, ontoRef);
                forgotEpisodic.addAll(score.forgetItem());
            }
        }

        public void updateTimes() {
            updateTimes(toBeForgottenEpisodic, SCORE.SCORE_PROP_TIMES_TO_BE_FORGOTTEN);
            updateTimes(toBeForgottenSemantic, SCORE.SCORE_PROP_TIMES_TO_BE_FORGOTTEN);
            updateTimes(lowScoreEpisodic, SCORE.SCORE_PROP_TIMES_LOW_SCORE);
            updateTimes(lowScoreSemantic, SCORE.SCORE_PROP_TIMES_LOW_SCORE);
            ontoRef.synchronizeReasoner();

        }

        private void updateLists() {
            ontoRef.synchronizeReasoner();
            lowScoreEpisodic.clear();
            lowScoreSemantic.clear();
            forgotEpisodic.clear();
            forgotSemantic.clear();
            toBeForgottenEpisodic.clear();
            toBeForgottenSemantic.clear();
            forgotClassSemantic.readSemantic();
            MORAxioms.Individuals indsForgotSemantic = forgotClassSemantic.getIndividualClassified();
            System.out.println(forgotClassSemantic);
            for (OWLNamedIndividual i : indsForgotSemantic) {
                forgotSemantic.add(i.toStringID().substring(SCORE.SCORE_IRI_ONTO.length() + 1));
            }

            forgotClassEpisodic.readSemantic();
            System.out.println(forgotClassEpisodic);
            MORAxioms.Individuals indsForgotEpisodic = forgotClassEpisodic.getIndividualClassified();
            for (OWLNamedIndividual i : indsForgotEpisodic) {
                forgotEpisodic.add(i.toStringID().substring(SCORE.SCORE_IRI_ONTO.length() + 1));
            }

            toBeForgottenClassEpisodic.readSemantic();
            System.out.println(toBeForgottenClassEpisodic);
            MORAxioms.Individuals indsForgottenEpisodic = toBeForgottenClassEpisodic.getIndividualClassified();
            for (OWLNamedIndividual i : indsForgottenEpisodic) {
                toBeForgottenEpisodic.add(i.toStringID().substring(SCORE.SCORE_IRI_ONTO.length() + 1));
            }

            toBeForgottenClassSemantic.readSemantic();
            System.out.println(toBeForgottenClassSemantic);
            MORAxioms.Individuals indsForgottenSemantic = toBeForgottenClassSemantic.getIndividualClassified();
            for (OWLNamedIndividual i : indsForgottenSemantic) {
                toBeForgottenSemantic.add(i.toStringID().substring(SCORE.SCORE_IRI_ONTO.length() + 1));
            }

            lowScoreClassEpisodic.readSemantic();
            System.out.println(lowScoreClassEpisodic);
            MORAxioms.Individuals indsLowScoreEpisodic = lowScoreClassEpisodic.getIndividualClassified();
            System.out.println(indsLowScoreEpisodic);
            for (OWLNamedIndividual i : indsLowScoreEpisodic) {
                lowScoreEpisodic.add(i.toStringID().substring(SCORE.SCORE_IRI_ONTO.length() + 1));
            }

            lowScoreClassSemantic.readSemantic();
            System.out.println(lowScoreClassSemantic);
            MORAxioms.Individuals indsLowScoreSemantic = lowScoreClassSemantic.getIndividualClassified();
            System.out.println(indsLowScoreSemantic);
            for (OWLNamedIndividual i : indsLowScoreSemantic) {
                lowScoreSemantic.add(i.toStringID().substring(SCORE.SCORE_IRI_ONTO.length() + 1));
            }
            //removing the common elements to the lists
            System.out.println("important, !!!!!!!!!!!Before removing common elements");
            System.out.println("\n \n " +lowScoreSemantic);
            System.out.println("\n \n " +lowScoreEpisodic);
            System.out.println("\n \n " +toBeForgottenSemantic);
            System.out.println("\n \n " +toBeForgottenEpisodic);
            System.out.println("\n \n " +forgotEpisodic);
            System.out.println("\n \n " +forgotSemantic);

            List<String> commonElementsSemanticLowScore= new ArrayList<>();
            commonElementsSemanticLowScore.addAll(toBeForgottenSemantic);
            commonElementsSemanticLowScore.retainAll(lowScoreSemantic);
            System.out.println("common elements tobeforgotten low score semantic \n"+commonElementsSemanticLowScore);
            lowScoreSemantic.removeAll(commonElementsSemanticLowScore);

            List<String> commonElementsEpisodicLowScore= new ArrayList<>();
            commonElementsEpisodicLowScore.addAll(toBeForgottenEpisodic);
            commonElementsEpisodicLowScore.retainAll(lowScoreEpisodic);
            System.out.println("common elements tobeforgotten low score episodic \n"+commonElementsEpisodicLowScore);
            lowScoreEpisodic.removeAll(commonElementsEpisodicLowScore);


            List<String> commonElementsSemantic= new ArrayList<>();
            commonElementsSemantic.addAll(toBeForgottenSemantic);
            commonElementsSemantic.retainAll(forgotSemantic);
            System.out.println("common elements forgot tobeforgot semantic \n"+commonElementsSemantic);
            toBeForgottenSemantic.removeAll(commonElementsSemantic);

            List<String> commonElementsEpisodic= new ArrayList<>();
            commonElementsEpisodic.addAll(toBeForgottenEpisodic);
            commonElementsEpisodic.retainAll(forgotEpisodic);
            System.out.println("common elements forgot tobeforgot episodic \n"+commonElementsEpisodic);
            toBeForgottenEpisodic.removeAll(commonElementsEpisodic);

            //removing the common elements to the lists
            System.out.println("important, !!!!!!!!!!!After removing common elements");
            System.out.println("\n \n low Score Semantic " +lowScoreSemantic);
            System.out.println("\n \n low Score Episoid " +lowScoreEpisodic);
            System.out.println("\n \n to Be forgotten Semantic" +toBeForgottenSemantic);
            System.out.println("\n \n to Be forgotten Episodic" +toBeForgottenEpisodic);
            System.out.println("\n \n forgot Episodic " +forgotEpisodic);
            System.out.println("\n \n forgot Semantic" +forgotSemantic);

        }

        public void updateTimes(List<String> names, String Property) {
            if (names.isEmpty()) {
                System.out.println("nothing to update");
                return;
            }
            for (String s : names) {
                MORFullIndividual ind = new MORFullIndividual(s,
                        ontoRef);
                ind.readSemantic();
                int number = ind.getLiteral(
                        Property).parseInteger();
                number++;
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

        public void objectPropertyValues(MORAxioms.ObjectSemantics objProp, String property, List<String> individuals) {
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

    public void objectPropertyValues(MORAxioms.ObjectSemantics objProp, String property, List<String> individuals) {
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

    public class ScoreCounter {

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
        /**
         * @return the type of this object (described as an ontological class).
         */
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
    public class ScoreCounterArray extends HashSet<ScoreCounter> {

        private ArrayList<sit_msgs.ScoreCounter> mapInROSMsg(ConnectedNode node){
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

    public ScoreCounter createToBeForgettingItem(String name, OWLReferences ontoRef,String NameTimesProperty){
        ScoreCounter score = new ScoreCounter(name);
        MORFullIndividual scoreInd= new MORFullIndividual(name, ontoRef);
        scoreInd.readSemantic();
        if(!NameTimesProperty.equals(SCORE.SCORE_PROP_TIMES_FORGOTTEN)) {
            score.setCounter(scoreInd.getLiteral(NameTimesProperty).parseInteger());
        }
        score.setScoreValue(scoreInd.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat());
        return score;
    }

    public void changeUserNoForget(String Name, OWLReferences ontoRef, boolean state){
        MORFullIndividual individual = new MORFullIndividual(Name,ontoRef);
        individual.readSemantic();
        individual.removeData(SCORE.SCORE_PROP_USER_NO_FORGET);
        if(state){
            individual.addData(SCORE.SCORE_PROP_USER_NO_FORGET,true, true);
        }
        else{
            individual.addData(SCORE.SCORE_PROP_USER_NO_FORGET,false,true);
        }
        individual.writeSemantic();
        individual.saveOntology(SCORE.SCORE_FILE_PATH);
    }
    public void resetCounter (String name, OWLReferences ontoRef){
        MORFullIndividual ind = new MORFullIndividual(name,ontoRef);
        ind.readSemantic();
        ind.removeData(SCORE.SCORE_PROP_TIMES_LOW_SCORE);
        ind.addData(SCORE.SCORE_PROP_TIMES_LOW_SCORE,0);
        ind.removeData(SCORE.SCORE_PROP_TIMES_TO_BE_FORGOTTEN);
        ind.addData(SCORE.SCORE_PROP_TIMES_TO_BE_FORGOTTEN,0);
        ind.removeData(SCORE.SCORE_PROP_TIMES_TO_BE_FORGOTTEN);
        ind.addData(SCORE.SCORE_PROP_TIMES_TO_BE_FORGOTTEN,0);
        ind.writeSemantic();
        ind.saveOntology(SCORE.SCORE_FILE_PATH);
    }
}

