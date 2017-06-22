package it.emarolab.scene_identification_tagging;

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
                    SemanticScore semanticScore = new SemanticScore(semantic.getName(), semantic.getSubClasses(), semantic.getSuperClasses(),ontoRef);
                    semanticScore.semanticInitialization();
                }
                if(!episodic.getName().isEmpty()){
                    EpisodicScore episodicScore= new EpisodicScore(episodic.getName(),episodic.getNameSemanticItem(),ontoRef);
                    episodicScore.episodicInitialization();

                }

            }



        };


    }
    private class SemanticScore{
        private String Name;
        private List<String> subClasses;
        private List<String> superClasses;
        MORFullIndividual scoreSemantic;
        MORFullIndividual totalScoreSemantic;
        MORFullIndividual totalScoreEpisodic;
        OWLReferences ontoRef;

        public SemanticScore(String Name,List<String> SubClasses,List<String> SuperClasses,OWLReferences ontoRef){
            scoreSemantic = new MORFullIndividual(Name,
                    ontoRef);
            totalScoreSemantic = new MORFullIndividual(SCORE.SCORE_INDIVIDUAL_TOTAL_SEMANTIC,
                    ontoRef);
            totalScoreEpisodic = new MORFullIndividual(SCORE.SCORE_INDIVIDUAL_TOTAL_EPISODIC,
                    ontoRef);
            this.ontoRef=ontoRef;
            this.Name=Name;
            this.subClasses=SubClasses;
            this.superClasses=SuperClasses;
        }

        public SemanticScore(String Name, OWLReferences ontoRef){
            scoreSemantic = new MORFullIndividual(Name,
                    ontoRef);
            totalScoreSemantic = new MORFullIndividual(SCORE.SCORE_INDIVIDUAL_TOTAL_SEMANTIC,
                    ontoRef);
            totalScoreEpisodic = new MORFullIndividual(SCORE.SCORE_INDIVIDUAL_TOTAL_EPISODIC,
                    ontoRef);
            this.ontoRef=ontoRef;
            this.Name=Name;

        }
        public void semanticInitialization(){
            // add the individual to the class
            System.out.println( "added individual to the class "+ SCORE.SCORE_CLASS_SEMANTIC_SCORE );
            scoreSemantic.readSemantic();
            scoreSemantic.addTypeIndividual(SCORE.SCORE_CLASS_SEMANTIC_SCORE);
            scoreSemantic.saveOntology(SCORE.SCORE_FILE_PATH);
            scoreSemantic.writeSemantic();
            scoreSemantic.readSemantic();
            // add the corresponding data properties
            //TODO you can define an individual score which does this by itself(connected to the ontology)
            System.out.println("Adding data properties ...");
            scoreSemantic.addData(SCORE.SCORE_PROP_NUMBER_BELONGING_INDIVIDUAL,0);
            scoreSemantic.addData(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL,0.0);
            scoreSemantic.addData(SCORE.SCORE_PROP_NUMBER_RETRIEVAL,1);
            scoreSemantic.addData(SCORE.SCORE_PROP_NUMBER_SUB_CLASSES,subClasses.size());
            scoreSemantic.addData(SCORE.SCORE_PROP_SCORE_SUM_SUB_CLASSES,computeSubClassesScore());
            System.out.println(computeSubClassesScore());
            scoreSemantic.addData(SCORE.SCORE_PROP_TIMES_FORGOTTEN,0);
            scoreSemantic.addData(SCORE.SCORE_PROP_TIMES_LOW_SCORE,0);
            scoreSemantic.addData(SCORE.SCORE_PROP_USER_NO_FORGET,false,true);
            scoreSemantic.writeSemantic();
            scoreSemantic.readSemantic();
            ontoRef.synchronizeReasoner();
            //compute the score
            System.out.println("Computing and adding the score...");
            float scoreComputed=computeScore(scoreSemantic);
            //add the score to the individual
            scoreSemantic.addData(SCORE.SCORE_PROP_HAS_SCORE,scoreComputed);
            scoreSemantic.writeSemantic();

            //Updating total score
            System.out.println("Updating total score..");
            System.out.println("UPDATING TOTAL IN SCORE INITIALIZATION");
            UpdateTotalSemanticScore( scoreComputed);
            //adding the property is superClassOf
                for (String s : subClasses) {
                    scoreSemantic.addObject(SCORE.SCORE_OBJ_PROP_IS_SUPER_CLASS_OF, s);
                }
            //adding data prop is subclass of
                for (String s : superClasses) {
                    scoreSemantic.addObject(SCORE.SCORE_OBJ_PROP_IS_SUB_CLASS_OF, s);
                }
            scoreSemantic.writeSemantic();

            scoreSemantic.saveOntology(SCORE.SCORE_FILE_PATH);
            //updating super class score
            System.out.println("updating super classes score...");
            updateSuperClassScore(superClasses,(float) scoreComputed);
        }
        //function which compute the sum of the score of the subclasses
        //input: subclass names
        public  float  computeSubClassesScore(){
            //if the set is empty hence there is no subclass return 0
            System.out.println("SUB CLASSES");
            System.out.println(subClasses.toString());
            System.out.println("\nbool");
            System.out.println(subClasses.isEmpty());
            if(this.subClasses.isEmpty()){
                return 0;
            }
            float total=0;
            //for all the subclasses
            for(String nameSubClass:subClasses){
                MORFullIndividual ind= new MORFullIndividual(nameSubClass, ontoRef);
                //read the current state of the individual
                ind.readSemantic();
                //adding to the total the value of dataproperty hasScore
                total+=ind.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat();
            }
            //return the total just computed
            return total;
        }
        private float computeScore(MORFullIndividual ind){
            //read the current state of the ontology
            ind.readSemantic();
            totalScoreSemantic.readSemantic();
            totalScoreEpisodic.readSemantic();
            float scoreSubClasses;
            float scoreIndividual;
            int retrieval= ind.getLiteral(SCORE.SCORE_PROP_NUMBER_RETRIEVAL).parseInteger();
            int numberBelongingIndividual=ind.getLiteral(SCORE.SCORE_PROP_NUMBER_BELONGING_INDIVIDUAL).parseInteger();
            int numberSubClasses= ind.getLiteral(SCORE.SCORE_PROP_NUMBER_SUB_CLASSES).parseInteger();
            //if the total semantic is equal to 0
            //TODO alla fine hai deciso di dividerlo per il numero di persone ecc perchè cosi e ceoerente se no in qualunque caso
            //sarebbe stato uno score sbagliato che avresti dovuto computare ogni volta che cambiava qualunque cosa
            // l'unica soluzione potrebbe essere computare lo score direttamente antraverso una SWRL rule
            ontoRef.synchronizeReasoner();

           if(numberSubClasses==0){
                scoreSubClasses=0;
            }
           else {
                scoreSubClasses=ind.getLiteral(SCORE.SCORE_PROP_SCORE_SUM_SUB_CLASSES).parseFloat()/scoreSubClasses;
            }
            // if the total episodic is equal to 0

            if(numberBelongingIndividual==0) {
                scoreIndividual= 0;
            }
            else{
                scoreIndividual=ind.getLiteral(SCORE.SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL).parseFloat()/numberBelongingIndividual;
            }
            ind.removeData(SCORE.SCORE_PROP_SCORE_SUB_CLASSES);
            ind.removeData(SCORE.SCORE_PROP_SCORE_BELONGING_INDIVIDUAL);
            ind.writeSemantic();
            return  ((float ) (SCORE.SCORE_SEMANTIC_WEIGHT_1 * numberBelongingIndividual +
                    SCORE.SCORE_SEMANTIC_WEIGHT_2 * scoreIndividual +
                    SCORE.SCORE_SEMANTIC_WEIGHT_3 * numberSubClasses +
                    SCORE.SCORE_SEMANTIC_WEIGHT_4 * scoreSubClasses +
                    SCORE.SCORE_SEMANTIC_WEIGHT_5 * retrieval));
        };
        //update the total semantic score  when a new item is added
        //input: score of the semantic item added
        public void UpdateTotalSemanticScore(float scoreComputed){
            //read the current state of the total semnatic score

            totalScoreSemantic.readSemantic();
            System.out.println("UPDATING TOTAL SEMANTIC SCORE!!!!!!!!!");
            //reading the data property has value
            float oldTotal=totalScoreSemantic.getLiteral(SCORE.SCORE_PROP_HAS_VALUE).parseFloat();
            System.out.println("\n\n OLD " +oldTotal);
            if(oldTotal==0.0){
                oldTotal=scoreComputed;
            }
            else{
                //change the value by adding the new score
                oldTotal+=scoreComputed;
            }
            System.out.println("\n\n NEW " +oldTotal);
            //change the dataproperty value
            totalScoreSemantic.removeData(SCORE.SCORE_PROP_HAS_VALUE);
            totalScoreSemantic.writeSemantic();
            totalScoreSemantic.addData(SCORE.SCORE_PROP_HAS_VALUE,oldTotal);
            totalScoreSemantic.writeSemantic();

        }
        //update the total semantic score when a score of an item has been changed
        //inputs :
        //-old score of the semantic item modified
        //-new score of the semantic item modified
        public void UpdateTotalSemanticScore(float oldScore, float newScore){
            //read the current state of total semantic item
            totalScoreSemantic.readSemantic();
            System.out.println("updatingTotalSemanticSCORE!!!!!!!!!!!!");
            //reading the value of hasValue dataproperty
            float total=totalScoreSemantic.getLiteral(SCORE.SCORE_PROP_HAS_VALUE).parseFloat();
            //updating the value
            System.out.println("\n\n old "+total);
            total-=oldScore;
            total+=newScore;
            System.out.println("\n\n\n\n new "+ total);
            //updating the data property with the new value just computed
            totalScoreSemantic.removeData(SCORE.SCORE_PROP_HAS_VALUE);
            totalScoreSemantic.writeSemantic();
            totalScoreSemantic.addData(SCORE.SCORE_PROP_HAS_VALUE,total);
            totalScoreSemantic.writeSemantic();


        }
        //updating superclasses score when a score of subclass has been changed
        //inputs:
        //-names of the superclasses to be updated
        //-old score of the updated semantic score
        //-new score of the updated semantic score
        public void updateSuperClassScore(List<String> classesNames, float scoreOld,float scoreNew){
            //if the set of string is empty hence there is no super class the functions
            //automatically returns
            if(classesNames.isEmpty()){
                return;
            }
            //for all the string
            for (String name:classesNames) {
                //define the MOR individual of such superclass
                MORFullIndividual superClass = new MORFullIndividual(
                        name,
                       ontoRef
                );
                //read the ontology
                superClass.readSemantic();
                //update the subclasses score with the new one
                float scoreSubClasses=superClass.getLiteral(SCORE.SCORE_PROP_SCORE_SUM_SUB_CLASSES).parseFloat();
                scoreSubClasses-=scoreOld;
                scoreSubClasses+=scoreNew;
                superClass.removeData(SCORE.SCORE_PROP_SCORE_SUM_SUB_CLASSES);
                superClass.addData(SCORE.SCORE_PROP_SCORE_SUM_SUB_CLASSES,scoreSubClasses);
                superClass.writeSemantic();
                //compute the new score
                System.out.println("COMPUTING SCORE FROM UPDATING SUPERCLASSES");
                float newScore=computeScore(superClass);
                //store the old score
                float oldScore=superClass.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat();
                //change the value of the data prop score
                superClass.removeData(SCORE.SCORE_PROP_HAS_SCORE);
                superClass.addData(SCORE.SCORE_PROP_HAS_SCORE,newScore);
                //write the semantic
                superClass.writeSemantic();

                superClass.readSemantic();
                //find the super classes of such element
                MORAxioms.ObjectSemantics objProp = superClass.getObjectSemantics();
                //check if there is any superclasses
                List<String> classes = new ArrayList<>();
                objectPropertyValues(objProp,SCORE.SCORE_OBJ_PROP_IS_SUB_CLASS_OF,classes);
                //update total semantic score
                System.out.println("UPDATING TOTAL IN UPDATING SUPER CLASSES");
                UpdateTotalSemanticScore(oldScore,newScore);
                //update superclasses score
                updateSuperClassScore(classes,oldScore,newScore);
            }
        }
        //update superclasses score when a new subclass has been added
        //inputs :
        //-name of the superclasses to be updated
        //-score of the semantic item added
        public void updateSuperClassScore(List<String> setName,float score){
            //if the set of string is empty hence there is no super class the functions
            //automatically returns

            if(setName.isEmpty()){
                return;
            }
            //for all the string
            for (String name:setName) {
                //define the MOR individual of such superclass
                MORFullIndividual superClass = new MORFullIndividual(
                        name,
                     ontoRef
                );
                //read the ontology
                superClass.readSemantic();
                //update the subclasses score with the new one
                float scoreSubClasses=superClass.getLiteral(SCORE.SCORE_PROP_SCORE_SUM_SUB_CLASSES).parseFloat();
                scoreSubClasses+=score;
                superClass.removeData(SCORE.SCORE_PROP_SCORE_SUM_SUB_CLASSES);
                superClass.addData(SCORE.SCORE_PROP_SCORE_SUM_SUB_CLASSES,scoreSubClasses);
                int numberSubClasses=superClass.getLiteral(SCORE.SCORE_PROP_NUMBER_SUB_CLASSES).parseInteger();
                numberSubClasses++;
                superClass.removeData(SCORE.SCORE_PROP_NUMBER_SUB_CLASSES);
                superClass.addData(SCORE.SCORE_PROP_NUMBER_SUB_CLASSES,numberSubClasses);
                superClass.writeSemantic();
                //compute the new score
                float newScore= computeScore(superClass);
                //store the old score
                float oldScore=superClass.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat();
                //change the value of the data prop score
                superClass.removeData(SCORE.SCORE_PROP_HAS_SCORE);
                superClass.addData(SCORE.SCORE_PROP_HAS_SCORE,newScore);
                //write the semantic
                superClass.writeSemantic();
                superClass.readSemantic();
                //find the super classes of such element
                MORAxioms.ObjectSemantics objProp = superClass.getObjectSemantics();
                //check if there is any subclasses
                List<String> classes = new ArrayList<>();
                objectPropertyValues(objProp,SCORE.SCORE_OBJ_PROP_IS_SUB_CLASS_OF,classes);
                //update total semantic score
                System.out.println("UPDATING TOTAL IN UPDATING SUPERCLASSES SCORE");
                UpdateTotalSemanticScore(oldScore,newScore);
                //update superclasses score
                updateSuperClassScore(classes,oldScore,newScore);
            }
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
            //Add obj Property
            scoreEpisodic.addObject(SCORE.SCORE_OBJ_PROP_IS_INDIVIDUAL_OF, SemanticItem.getName());
            //compute the score and add it to the individual
            float scoreComputed = computeScore(0, 1);
            scoreEpisodic.addData(SCORE.SCORE_PROP_HAS_SCORE, scoreComputed);
            //write the semantic
            scoreEpisodic.writeSemantic();
            scoreEpisodic.readSemantic();
            System.out.println("added data prop");
            scoreEpisodic.writeSemantic();
            System.out.println("added score property");
            updateTotalEpisodicScore(scoreComputed);
            System.out.println("updatinf semantic from individual");
            updateSemanticFromIndividual(Name, scoreComputed);
            scoreEpisodic.saveOntology(SCORE.SCORE_FILE_PATH);
        }

        private float computeScore(int semantic_retrieval,
                                   int episodic_retrieval) {
            return ((float) (SCORE.SCORE_EPISODIC_WEIGHT_1 * semantic_retrieval +
                    SCORE.SCORE_EPISODIC_WEIGHT_2 * episodic_retrieval));


        }

        public void updateTotalEpisodicScore(float score){
            totalScoreEpisodic.readSemantic();
            float total=totalScoreEpisodic.getLiteral(SCORE.SCORE_PROP_HAS_VALUE).parseFloat();
            if(total<0){
                total=score;
            }
            else {
                total += score;
            }
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
            System.out.println("Updating score  semantic from individual");
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
            System.out.println("writing the semantic and computing the new score...");
            System.out.println("UPDATING TOTAL IN UPDATING SEMANTIC FROM INDIVIDUAL !!!");
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
