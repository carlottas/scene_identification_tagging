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
    private static final String ONTO_NAME = "ONTO_NAME"; // an arbritary name to refer the ontology
    private static final String NAME = "testScene";
   /* MORFullIndividual scoreEpisodic;
    MORFullIndividual scoreSemantic;
    MORFullIndividual totalScoreEpisodic;
    MORFullIndividual totalScoreSemantic;
*/
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

                if(!semantic.getName().isEmpty()) {
                    SemanticScore semanticScore = new SemanticScore(semantic.getName(), semantic.getSubClasses(), semantic.getSuperClasses());
                    semanticScore.semanticInitialization();
                }


            }



        };


    }

   // public void setup(String EpisodicName, String SemanticName){
     //   if(!EpisodicName.isEmpty()) {
       //     scoreEpisodic = new MORFullIndividual(EpisodicName,
    //           SCORE.SCORE_ONTO_NAME,
       /*             SCORE.SCORE_FILE_PATH,
                    SCORE.SCORE_IRI_ONTO);
        }
        if(!SemanticName.isEmpty()) {
            scoreSemantic = new MORFullIndividual(SemanticName,
                    SCORE.SCORE_ONTO_NAME,
                    SCORE.SCORE_FILE_PATH,
                    SCORE.SCORE_IRI_ONTO);
        }
        if(!EpisodicName.isEmpty()|| !SemanticName.isEmpty()) {
            //set up of the total score
            totalScoreSemantic = new MORFullIndividual(SCORE.SCORE_INDIVIDUAL_TOTAL_SEMANTIC,
                    SCORE.SCORE_ONTO_NAME,
                    SCORE.SCORE_FILE_PATH,
                    SCORE.SCORE_IRI_ONTO);
            //set up of the total score
            totalScoreEpisodic = new MORFullIndividual(SCORE.SCORE_INDIVIDUAL_TOTAL_EPISODIC,
                    SCORE.SCORE_ONTO_NAME,
                    SCORE.SCORE_FILE_PATH,
                    SCORE.SCORE_IRI_ONTO);
        }

    }
    */

    private class SemanticScore{
        private String Name;
        private List<String> subClasses;
        private List<String> superClasses;
        MORFullIndividual scoreSemantic;
        MORFullIndividual totalScoreSemantic;
        MORFullIndividual totalScoreEpisodic;
        OWLReferences ontoRef;


        public SemanticScore(String Name,List<String> subClasses,List<String> superClasses){
            this.Name=Name;
            scoreSemantic = new MORFullIndividual(Name,
                    SCORE.SCORE_ONTO_NAME,
                    SCORE.SCORE_FILE_PATH,
                    SCORE.SCORE_IRI_ONTO);
            totalScoreSemantic = new MORFullIndividual(SCORE.SCORE_INDIVIDUAL_TOTAL_SEMANTIC,
                    SCORE.SCORE_ONTO_NAME,
                    SCORE.SCORE_FILE_PATH,
                    SCORE.SCORE_IRI_ONTO);
            totalScoreEpisodic = new MORFullIndividual(SCORE.SCORE_INDIVIDUAL_TOTAL_EPISODIC,
                    SCORE.SCORE_ONTO_NAME,
                    SCORE.SCORE_FILE_PATH,
                    SCORE.SCORE_IRI_ONTO);
            this.ontoRef=OWLReferencesInterface.OWLReferencesContainer.newOWLReferenceFromFileWithPellet(
                    SCORE.SCORE_ONTO_NAME, SCORE.SCORE_FILE_PATH, SCORE.SCORE_IRI_ONTO, true);
            this.subClasses=subClasses;
            this.superClasses=superClasses;
        }
        public SemanticScore(String Name){
            scoreSemantic = new MORFullIndividual(Name,
                    SCORE.SCORE_ONTO_NAME,
                    SCORE.SCORE_FILE_PATH,
                    SCORE.SCORE_IRI_ONTO);
            totalScoreSemantic = new MORFullIndividual(SCORE.SCORE_INDIVIDUAL_TOTAL_SEMANTIC,
                    SCORE.SCORE_ONTO_NAME,
                    SCORE.SCORE_FILE_PATH,
                    SCORE.SCORE_IRI_ONTO);
            totalScoreEpisodic = new MORFullIndividual(SCORE.SCORE_INDIVIDUAL_TOTAL_EPISODIC,
                    SCORE.SCORE_ONTO_NAME,
                    SCORE.SCORE_FILE_PATH,
                    SCORE.SCORE_IRI_ONTO);
            this.ontoRef=OWLReferencesInterface.OWLReferencesContainer.newOWLReferenceFromFileWithPellet(
                    SCORE.SCORE_ONTO_NAME, SCORE.SCORE_FILE_PATH, SCORE.SCORE_IRI_ONTO, true);
        }
        public SemanticScore(String Name,List<String> SubClasses,List<String> SuperClasses,OWLReferences ontoRef){
            this.Name=Name;
            scoreSemantic = new MORFullIndividual(Name,
                    ontoRef);
            totalScoreSemantic = new MORFullIndividual(SCORE.SCORE_INDIVIDUAL_TOTAL_SEMANTIC,
                    ontoRef);
            totalScoreEpisodic = new MORFullIndividual(SCORE.SCORE_INDIVIDUAL_TOTAL_EPISODIC,
                    ontoRef);
            this.ontoRef=ontoRef;
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
        }
        public void semanticInitialization(){
            // add the individual to the class
            System.out.println( "added individual to the class "+ SCORE.SCORE_CLASS_SEMANTIC_SCORE );
            scoreSemantic.readSemantic();
            scoreSemantic.addTypeIndividual(SCORE.SCORE_CLASS_SEMANTIC_SCORE);
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
            scoreSemantic.addData(SCORE.SCORE_PROP_TIMES_FORGOTTEN,0);
            scoreSemantic.addData(SCORE.SCORE_PROP_TIMES_LOW_SCORE,0);
            scoreSemantic.addData(SCORE.SCORE_PROP_USER_NO_FORGET,false,true);
            scoreSemantic.writeSemantic();
            ontoRef.synchronizeReasoner();
            scoreSemantic.readSemantic();
            //compute the score
            System.out.println("Computing and adding the score...");
            float scoreComputed=computeScore(scoreSemantic);
            //add the score to the individual
            scoreSemantic.addData(SCORE.SCORE_PROP_HAS_SCORE,scoreComputed);
            scoreSemantic.writeSemantic();
            ontoRef.synchronizeReasoner();
            //Updating total score
            System.out.println("Updating total score..");
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
            ontoRef.synchronizeReasoner();
            //updating super class score
            System.out.println("updating super classes score...");
            updateSuperClassScore(superClasses,(float) scoreComputed);
        }
        //function which compute the sum of the score of the subclasses
        //input: subclass names
        public  float  computeSubClassesScore(){
            //if the set is empty hence there is no subclass return 0
            if(this.subClasses.isEmpty()){
                return 0;
            }
            float total=0;
            //for all the subclasses
            for(String nameSubClass:subClasses){
                MORFullIndividual ind= new MORFullIndividual(nameSubClass,
                        ontoRef);
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
            //TODO test this part cuz it is different from the first one
            if(totalScoreSemantic.getLiteral(SCORE.SCORE_PROP_HAS_VALUE).parseFloat()==0.0){
                scoreSubClasses=0;
            }
            else {
                scoreSubClasses=ind.getLiteral(SCORE.SCORE_PROP_SCORE_SUM_SUB_CLASSES).parseFloat();

            }
            // if the total episodic is equal to 0
            if(totalScoreEpisodic.getLiteral(SCORE.SCORE_PROP_HAS_VALUE).parseFloat()==0.0) {
                scoreIndividual= 1;
            }
            else{
                scoreIndividual=ind.getLiteral(SCORE.SCORE_PROP_SCORE_BELONGING_INDIVIDUAL).parseFloat();
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
            ontoRef.synchronizeReasoner();
            totalScoreSemantic.readSemantic();
            ontoRef.synchronizeReasoner();
            //reading the data property has value
            float oldTotal=totalScoreSemantic.getLiteral(SCORE.SCORE_PROP_HAS_VALUE).parseFloat();
            if(oldTotal==0.0){
                oldTotal=scoreComputed;
            }
            else{
                //change the value by adding the new score
                oldTotal+=scoreComputed;
            }

            //change the dataproperty value
            totalScoreSemantic.removeData(SCORE.SCORE_PROP_HAS_VALUE);
            totalScoreSemantic.writeSemantic();
            totalScoreSemantic.addData(SCORE.SCORE_PROP_HAS_VALUE,oldTotal);
            totalScoreSemantic.writeSemantic();
            ontoRef.synchronizeReasoner();
        }
        //update the total semantic score when a score of an item has been changed
        //inputs :
        //-old score of the semantic item modified
        //-new score of the semantic item modified
        public void UpdateTotalSemanticScore(float oldScore, float newScore){
            //read the current state of total semantic item
            ontoRef.synchronizeReasoner();
            totalScoreSemantic.readSemantic();
            //reading the value of hasValue dataproperty
            float total=totalScoreSemantic.getLiteral(SCORE.SCORE_PROP_HAS_VALUE).parseFloat();
            //updating the value
            total-=oldScore;
            total+=newScore;
            //updating the data property with the new value just computed
            totalScoreSemantic.removeData(SCORE.SCORE_PROP_HAS_VALUE);
            totalScoreSemantic.writeSemantic();
            totalScoreSemantic.addData(SCORE.SCORE_PROP_HAS_VALUE,total);
            totalScoreSemantic.writeSemantic();
            ontoRef.synchronizeReasoner();

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
                float newScore=computeScore(superClass);
                //store the old score
                float oldScore=superClass.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat();
                //change the value of the data prop score
                superClass.removeData(SCORE.SCORE_PROP_HAS_SCORE);
                superClass.addData(SCORE.SCORE_PROP_HAS_SCORE,newScore);
                //write the semantic
                superClass.writeSemantic();
                ontoRef.synchronizeReasoner();
                superClass.readSemantic();
                //find the super classes of such element
                MORAxioms.ObjectSemantics objProp = superClass.getObjectSemantics();
                //check if there is any superclasses
                List<String> classes = new ArrayList<>();
                objectPropertyValues(objProp,SCORE.SCORE_OBJ_PROP_IS_SUB_CLASS_OF,classes);
                //update total semantic score
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
                UpdateTotalSemanticScore(oldScore,newScore);
                //update superclasses score
                updateSuperClassScore(classes,oldScore,newScore);
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
        public void setSubClasses(List<String> subClasses){this.subClasses=subClasses;}
        public List<String> getSubClasses(){return this.subClasses;}
        public void setSuperClasses(List<String> superClasses){this.superClasses=superClasses;}
        public List<String> getSuperClasses(){return this.superClasses;}
        public MORFullIndividual getScoreSemantic(){return this.scoreSemantic;}
        public MORFullIndividual getTotalScoreSemantic(){return this.totalScoreSemantic;}
        public OWLReferences getOntoRef(){return this.ontoRef;}
        public void setOntoRef(OWLReferences ontoRef){this.ontoRef=ontoRef;}

    }

}
