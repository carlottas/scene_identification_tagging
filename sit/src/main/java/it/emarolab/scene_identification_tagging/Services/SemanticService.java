
package it.emarolab.scene_identification_tagging.Services;

import it.emarolab.owloop.aMORDescriptor.utility.individual.MORFullIndividual;
import it.emarolab.scene_identification_tagging.sceneRepresentation.*;
import sit_msgs.*;
import org.ros.node.ConnectedNode;
import org.ros.node.service.ServiceResponseBuilder;
import org.ros.namespace.GraphName;
import it.emarolab.amor.owlInterface.OWLReferences;
import it.emarolab.scene_identification_tagging.realObject.*;
import it.emarolab.scene_identification_tagging.owloopDescriptor.retrievalDescriptor;
import java.util.*;
import it.emarolab.scene_identification_tagging.Interfaces.*;
public class SemanticService
        extends  ROSSemanticInterface.ROSSemanticServer<SemanticInterfaceRequest, SemanticInterfaceResponse>
    implements SITBase, MemoryInterface {
    private static final String SERVICE_NAME = "SemanticService";
    private static final String ONTO_NAME = "ONTO_NAME"; // an arbritary name to refer the ontology

    public boolean initParam(ConnectedNode node) {

        // stat the service
        node.newServiceServer(
                getServerName(), // set service name
                SemanticInterface._TYPE, // set ROS service message
                getService(node) // set ROS service response
        );
       loadSemantics(ONTO_NAME,ONTO_FILE,ONTO_IRI);
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

    public ServiceResponseBuilder<SemanticInterfaceRequest, SemanticInterfaceResponse> getService(ConnectedNode node) {
        return new ServiceResponseBuilder<SemanticInterfaceRequest, SemanticInterfaceResponse>() {

            /**
             * This object is used to react to {@link SceneService} call,
             * it defines the computation to be performed.
             *
             * @param request  an initialised ROS message for the server request
             * @param response the ROS message server response, to be set.
             */
            @Override
            public void
            build(SemanticInterfaceRequest request, SemanticInterfaceResponse response) {
                // load ontology
                System.out.println("loading the ontology ");
                 OWLReferences ontoRef = getOntology();
                System.out.println(ontoRef);
                // suppress aMOR log
                it.emarolab.amor.owlDebugger.Logger.setPrintOnConsole(false);
                int decision = request.getDecision();
                //MEMORIZATION
                if (decision == 1) {
                    // initialise objects
                    Set<GeometricPrimitive> objects = memory.fromPITtoSIT(request.getGeometricPrimitives(), ontoRef);
                    // add objects
                    for (GeometricPrimitive i : objects) {
                        for (GeometricPrimitive j : objects)
                            if (!i.equals(j))
                                j.addDisjointIndividual(i.getInstance());
                        i.getObjectSemantics().clear(); // clean previus spatial relation
                        i.writeSemantic();
                    }
                    // run SWRL
                    ontoRef.synchronizeReasoner();
                    // get SWRL results
                    //should get the semantic
                    for (GeometricPrimitive i : objects) {
                        // it could be implemented faster
                        i.readSemantic();
                    }

                    // create scene and reason for recognition
                    SceneRepresentation recognition1 = new SceneRepresentation(objects, ontoRef);
                    System.out.println("Recognised with best confidence: " + recognition1.getRecognitionConfidence() + " should learn? " + recognition1.shouldLearn());
                    System.out.println("Best recognised class: " + recognition1.getBestRecognitionDescriptor());
                    System.out.println("Other recognised classes: " + recognition1.getSceneDescriptor().getTypeIndividual());

                    // learn the new scene if is the case
                    if (recognition1.shouldLearn()) {
                        response.setLearnt(true);
                        System.out.println("Learning.... ");
                        recognition1.learn(memory.computeSceneName(ontoRef));
                    } else {
                        response.setLearnt(false);
                    }

                    ontoRef.synchronizeReasoner();
                    //filling the response
                    List<String> subClasses = recognition1.getBestRecognitionDescriptor().SubConceptToString();
                    List<String> superClasses = recognition1.getBestRecognitionDescriptor().SuperConceptToString();
                    response.setSceneName(recognition1.getBestRecognitionDescriptor().NameToString(ONTO_NAME.length() + 1));
                    response.setSubClasses(subClasses);
                    response.setSuperClasses(superClasses);
                    ontoRef.synchronizeReasoner();
                    recognition1.getBestRecognitionDescriptor().readSemantic();
                /*
                //check whether is actually working correctly
                List<String> isFirstSupClassOf=computeIsFirstSuperClassOf(subClasses,ontoRef,recognition1);
                recognition1.getBestRecognitionDescriptor().readSemantic();
                List<String > firstSupClass= computeFirstSuperClass(recognition1,ontoRef);
                System.out.println("first sup class \n" +firstSupClass);
                System.out.println("is first sup class\n"+isFirstSupClassOf);
                response.setFirstSuperClass(firstSupClass);
                response.setIsFirstSuperClassOf(isFirstSupClassOf);
                */
                    Atoms atoms = memory.fromSemanticToEpisodic(objects,ONTO_NAME);
                    atoms.mapInROSMsg(node, response);
                    ontoRef.removeIndividual(recognition1.getSceneDescriptor().getInstance());
                    for (GeometricPrimitive i : objects)
                        ontoRef.removeIndividual(i.getInstance());
                    ontoRef.synchronizeReasoner();
                    recognition1.getBestRecognitionDescriptor().saveOntology(ONTO_FILE);
                }
                //retrieval
                else if (decision==2){
                    if(!request.getRetrieval().isEmpty()) {
                        retrievalDescriptor retrievalDescriptor = new retrievalDescriptor(request.getRetrieval(), ontoRef);
                        List<String> ListRetrieval = new ArrayList<>();
                        ListRetrieval.addAll(retrievalDescriptor.getNameRetrieval());
                        retrievalDescriptor.updateTimeRetrievalForgotten();
                        retrievalDescriptor.removeToBeForgot();
                        response.setRetrievaled(ListRetrieval);
                        //System.out.println("removing class");
                        //ontoRef.removeClass(RETRIEVAL.SEMANTIC_RETRIEVAL_NAME);
                        //ontoRef.saveOntology(ONTO_FILE);
                        response.setUserNoForget(retrievalDescriptor.getUserNoForget());
                        response.setResetCounter(retrievalDescriptor.getResetCounter());



                    }

                }
                //forgetting
                else if (decision==3){
                    //TODO check whether re classification occurs

                    /*
                    ontoRef.synchronizeReasoner();
                    MORFullConcept scene0= new MORFullConcept("Scene0",ontoRef);
                    scene0.readSemantic();
                    response.setSceneName(scene0.getIndividualClassified().toString());MORFullConcept scene1= new MORFullConcept("Scene1",ontoRef);
                    scene1.readSemantic();
                    response.setResetCounter(Arrays.asList(scene1.getIndividualClassified().toString().split(" ")));

                    MORFullConcept scene2= new MORFullConcept("Scene2",ontoRef);
                    scene2.readSemantic();
                    response.setUserNoForget(Arrays.asList(scene2.getIndividualClassified().toString().split(" ")));
                    MORFullConcept scene3= new MORFullConcept("Scene3",ontoRef);
                    scene3.readSemantic();
                    response.setFirstSuperClass(Arrays.asList(scene3.getIndividualClassified().toString().split(" ")));
                    MORFullConcept scene4= new MORFullConcept("Scene4",ontoRef);
                    scene4.readSemantic();
                    response.setIsFirstSuperClassOf(Arrays.asList(scene4.getIndividualClassified().toString().split(" ")));
*/


                }
                //recognition
                else if (decision==4){
                    // initialise objects
                    Set<GeometricPrimitive> objects = memory.fromPITtoSIT(request.getGeometricPrimitives(), ontoRef);
                    // add objects
                    for (GeometricPrimitive i : objects) {
                        for (GeometricPrimitive j : objects)
                            if (!i.equals(j))
                                j.addDisjointIndividual(i.getInstance());
                        i.getObjectSemantics().clear(); // clean previus spatial relation
                        i.writeSemantic();
                    }
                    // run SWRL
                    ontoRef.synchronizeReasoner();
                    // get SWRL results
                    //should get the semantic
                    for (GeometricPrimitive i : objects) {
                        // it could be implemented faster
                        i.readSemantic();
                    }

                    // create scene and reason for recognition
                    SceneRepresentation recognition1 = new SceneRepresentation(objects, ontoRef);
                    if(recognition1.shouldLearn()){
                        System.out.println("i have never seen something like this, i should learn it ");
                    }
                    else {
                        String nameScene= recognition1.getBestRecognitionDescriptor().NameToString(ONTO_NAME.length() + 1);
                        String individualName = FORGETTING.NAME_SEMANTIC_INDIVIDUAL + nameScene.replaceAll("Scene", "");
                        MORFullIndividual ind = new MORFullIndividual(individualName, ontoRef);
                        ind.readSemantic();
                        if (!ind.getLiteral(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT).parseBoolean()) {
                            System.out.println("Recognised with best confidence: " + recognition1.getRecognitionConfidence());
                            System.out.println("Best recognised class: " + recognition1.getBestRecognitionDescriptor());
                            System.out.println("Other recognised classes: " + recognition1.getSceneDescriptor().getTypeIndividual());
                            response.setSceneName(nameScene);
                            Atoms atoms = memory.fromSemanticToEpisodic(objects,ONTO_NAME);
                            atoms.mapInROSMsg(node, response);
                            ontoRef.removeIndividual(recognition1.getSceneDescriptor().getInstance());
                        }
                        else{
                                ind.readSemantic();
                                float counter = ind.getLiteral(FORGETTING.NAME_DATA_PROPERTY_RETRIEVAL_FORGOT).parseFloat();
                                float newCounter = 0;
                                if (counter<1){
                                    newCounter=counter+FORGETTING.INCREMENT_ONE;
                                    if (newCounter>=1){
                                        List<String> resetCounter= new ArrayList<>();
                                        resetCounter.add(nameScene);
                                        ind.removeData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT);
                                        ind.addData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT,false,true);
                                        ind.writeSemantic();
                                        response.setResetCounter(resetCounter);

                                    }

                                }
                                else if (counter<2 && counter>=1){
                                    newCounter=counter+FORGETTING.INCREMENT_TWO;
                                    if(newCounter>=2){
                                        List<String> resetCounter= new ArrayList<>();
                                        ind.removeData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT);
                                        ind.addData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT,false,true);
                                        ind.writeSemantic();
                                        resetCounter.add(nameScene);
                                        response.setResetCounter(resetCounter);
                                    }

                                }
                                else if (counter<3&&counter>=2){
                                    newCounter=counter+FORGETTING.INCREMENT_THREE;
                                    if(newCounter>=3){
                                        List<String> userNoForget= new ArrayList<>();
                                        ind.removeData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT);
                                        ind.addData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT,false,true);
                                        ind.writeSemantic();
                                        userNoForget.add(nameScene);
                                        response.setUserNoForget(userNoForget);
                                    }
                                }
                                ind.removeData(FORGETTING.NAME_DATA_PROPERTY_RETRIEVAL_FORGOT);
                                ind.addData(FORGETTING.NAME_DATA_PROPERTY_RETRIEVAL_FORGOT,newCounter);
                                ind.writeSemantic();
                                ind.saveOntology(ONTO_FILE);
                        }
                        for (GeometricPrimitive i : objects)
                            ontoRef.removeIndividual(i.getInstance());
                        ontoRef.synchronizeReasoner();
                    }
                }
               else if (decision==0){
                    //forget and put user No Forget
                    for(String s :request.getDeleteSemantic()){
                        //todo delete
                    }
                    for (String s : request.getToBeForget()){
                        MORFullIndividual toBeForget= new MORFullIndividual(FORGETTING.NAME_SEMANTIC_INDIVIDUAL + s.replaceAll("Scene", ""),ontoRef);
                        toBeForget.readSemantic();
                        toBeForget.removeData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT);
                        toBeForget.addData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT,true,true);
                        toBeForget.writeSemantic();
                        toBeForget.saveOntology(ONTO_FILE);
                    }
                }
            }

        };
    }

}



