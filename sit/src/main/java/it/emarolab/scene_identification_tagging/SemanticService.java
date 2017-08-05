
package it.emarolab.scene_identification_tagging;

import it.emarolab.owloop.aMORDescriptor.utility.individual.MORFullIndividual;
import it.emarolab.scene_identification_tagging.owloopDescriptor.SceneClassDescriptor;
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

/**
 * Ros service which manage the Semantic Ontology .
 * Depending on the user decisin it
 * Memorize new semantic items
 * Retrieve old semantic items
 * Recognize semantic item
 * Either force the forgetting or save items
 * Furthermore, after each retrieval, delete the elements which has been adressed by the score ontology.
 * In addition, during the retrieval, is able to force a forgotten element to be retrieved if the user insisit in
 * wanting such information
 */
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
                    //preparing the output for the episodic service
                    Atoms atoms = memory.fromSemanticToEpisodic(objects,ONTO_NAME);
                    //map the output in a ros message
                    atoms.mapInROSMsg(node, response);
                    //remove the ndividual used to recognize
                    ontoRef.removeIndividual(recognition1.getSceneDescriptor().getInstance());
                    //remove the individuals of geometri primitives
                    for (GeometricPrimitive i : objects)
                        ontoRef.removeIndividual(i.getInstance());
                    ontoRef.synchronizeReasoner();
                    recognition1.getBestRecognitionDescriptor().saveOntology(ONTO_FILE);
                }
                //retrieval
                else if (decision==2){
                    if(!request.getRetrieval().isEmpty()) {
                        //definition of the retrieval descriptor
                        retrievalDescriptor retrievalDescriptor = new retrievalDescriptor(request.getRetrieval(), ontoRef);
                        List<String> ListRetrieval = new ArrayList<>();
                        //getting the element that has been retrieved
                        ListRetrieval.addAll(retrievalDescriptor.getNameRetrieval());
                        //update the times that forgot elements has been retrieved
                        retrievalDescriptor.updateTimeRetrievalForgotten();
                        //remove the toBeForgot attribute to the elements that have been
                        //forced to be retrieved
                        retrievalDescriptor.removeToBeForgot();

                        //filling the response
                        response.setRetrievaled(ListRetrieval);
                        response.setUserNoForget(retrievalDescriptor.getUserNoForget());
                        response.setResetCounter(retrievalDescriptor.getResetCounter());



                    }

                }
                //forgetting
                else if (decision==3){
                }
                //recognition
                else if (decision==4) {
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
                    if (recognition1.shouldLearn()) {
                        System.out.println("i have never seen something like this, i should learn it ");
                    } else {
                        //if the element exists in the ontology
                        String nameScene = recognition1.getBestRecognitionDescriptor().NameToString(ONTO_NAME.length() + 1);
                        //check whether the element has been forgot
                        String individualName = FORGETTING.NAME_SEMANTIC_INDIVIDUAL + nameScene.replaceAll("Scene", "");
                        MORFullIndividual ind = new MORFullIndividual(individualName, ontoRef);
                        ind.readSemantic();
                        //if the element has not been forgot
                        if (!ind.getLiteral(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT).parseBoolean()) {
                            System.out.println("Recognised with best confidence: " + recognition1.getRecognitionConfidence());
                            System.out.println("Best recognised class: " + recognition1.getBestRecognitionDescriptor());
                            System.out.println("Other recognised classes: " + recognition1.getSceneDescriptor().getTypeIndividual());
                            //filling the response
                            response.setSceneName(nameScene);
                            //preparing the output for the episodic service
                            Atoms atoms = memory.fromSemanticToEpisodic(objects, ONTO_NAME);
                            //map into Ros Msg
                            atoms.mapInROSMsg(node, response);
                            //remove the individual used for recognition
                            ontoRef.removeIndividual(recognition1.getSceneDescriptor().getInstance());
                            //remove the geometric primitives
                            for (GeometricPrimitive i : objects)
                                ontoRef.removeIndividual(i.getInstance());
                            ontoRef.synchronizeReasoner();

                        }
                        //if the elements has been forgot
                        else {   //read the ontology
                            ind.readSemantic();
                            //read the counter value
                            float counter = ind.getLiteral(FORGETTING.NAME_DATA_PROPERTY_RETRIEVAL_FORGOT).parseFloat();
                            float newCounter = 0;
                            //update the counter
                            if (counter < 1) {
                                newCounter = counter + FORGETTING.INCREMENT_ONE;
                                //if the new counter value is bigger equal than one
                                if (newCounter >= 1) {
                                    //reset the counter in the score Ontology
                                    List<String> resetCounter = new ArrayList<>();
                                    resetCounter.add(nameScene);
                                    //retrieved the element
                                    ind.removeData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT);
                                    ind.addData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT, false, true);
                                    ind.writeSemantic();
                                    //filling the response
                                    response.setResetCounter(resetCounter);
                                }

                            }
                            //increment the counter
                            else if (counter < 2 && counter >= 1) {
                                newCounter = counter + FORGETTING.INCREMENT_TWO;
                                //if the counter is bigger equal than 2
                                if (newCounter >= 2) {
                                    //Update the ontology to allow retrieval
                                    List<String> resetCounter = new ArrayList<>();
                                    ind.removeData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT);
                                    ind.addData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT, false, true);
                                    ind.writeSemantic();
                                    //reset the counters
                                    resetCounter.add(nameScene);
                                    //filling the response
                                    response.setResetCounter(resetCounter);
                                }

                            }
                            //increment the counter
                            else if (counter < 3 && counter >= 2) {
                                newCounter = counter + FORGETTING.INCREMENT_THREE;
                                //if the new counter is equal bigger than 3
                                if (newCounter >= 3) {
                                    //Update the ontology to allow retrieval and
                                    //prevent the item to be again gorgot
                                    List<String> userNoForget = new ArrayList<>();
                                    ind.removeData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT);
                                    ind.addData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT, false, true);
                                    ind.writeSemantic();
                                    userNoForget.add(nameScene);
                                    response.setUserNoForget(userNoForget);
                                }
                            }
                            //update the ontology
                            ind.removeData(FORGETTING.NAME_DATA_PROPERTY_RETRIEVAL_FORGOT);
                            ind.addData(FORGETTING.NAME_DATA_PROPERTY_RETRIEVAL_FORGOT, newCounter);
                            ind.writeSemantic();
                            ind.saveOntology(ONTO_FILE);
                        }

                    }
                }
                //automatic forgetting
               else if (decision==0){
                    //forget and put user No Forget
                    for(String s :request.getDeleteSemantic()){
                        //todo delete
                        System.out.println("importante, rimuovi " +s);
                    }
                    //Update the ontology to prevent the elemnt forgot to be retrieved
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



