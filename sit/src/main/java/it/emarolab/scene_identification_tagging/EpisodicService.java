package it.emarolab.scene_identification_tagging;

import it.emarolab.amor.owlInterface.OWLReferences;
import it.emarolab.owloop.aMORDescriptor.MORAxioms;
import it.emarolab.owloop.aMORDescriptor.utility.individual.MORFullIndividual;
import it.emarolab.scene_identification_tagging.owloopDescriptor.SceneClassDescriptor;
import it.emarolab.scene_identification_tagging.realObject.*;
import it.emarolab.scene_identification_tagging.sceneRepresentation.*;
import javafx.scene.shape.*;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import sit_msgs.*;
import org.ros.node.ConnectedNode;
import org.ros.node.service.ServiceResponseBuilder;
import org.ros.namespace.GraphName;
import java.util.*;
import it.emarolab.scene_identification_tagging.sceneRepresentation.Atom;
import it.emarolab.scene_identification_tagging.Interfaces.*;

/**
 * ROS service which manipulates the episodic onotlogy
 * Depending on the user decision it is able to
 * -Memorize a new Episodic Item
 * -Retrieve old Episodic Item
 * -Recognize Episodic Item
 * -Either Force the forgetting or save an item
 * Furthermore the service is able to force the retrieval of an element forgot if the user
 * insist in the retrieval
 * In addition it automatically forgets episodic items depending on the score Ontology situation
 */
public class EpisodicService
        extends  ROSSemanticInterface.ROSSemanticServer<EpisodicInterfaceRequest, EpisodicInterfaceResponse>
        implements SITBase,MemoryInterface {

    private static final String SERVICE_NAME = "EpisodicService";


    public boolean initParam(ConnectedNode node) {

        // stat the service
        node.newServiceServer(
                getServerName(), // set service name
                EpisodicInterface._TYPE, // set ROS service message
                getService(node) // set ROS service response
        );
        loadSemantics(EPISODIC_ONTO_NAME,EPISODIC_ONTO_FILE,EPISODIC_ONTO_IRI);
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

    public ServiceResponseBuilder<EpisodicInterfaceRequest, EpisodicInterfaceResponse>
    getService(ConnectedNode node) {
        return new ServiceResponseBuilder<EpisodicInterfaceRequest, EpisodicInterfaceResponse>() {

            /**
             * This object is used to react to {@link EpisodicService} call,
             * it defines the computation to be performed.
             *
             * @param request  an initialised ROS message for the server request
             * @param response the ROS message server response, to be set.
             */
            @Override
            public void
            build(EpisodicInterfaceRequest request, EpisodicInterfaceResponse response) {
                //Ontology declaration
                OWLReferences ontoRef = getOntology();
                // suppress aMOR log
                it.emarolab.amor.owlDebugger.Logger.setPrintOnConsole(false);
                int decision = request.getDecision();
                //Memorization
                if (decision == 1) {
                    //get the semantic item name
                    String SceneName = request.getSceneName();
                    //getting the subclasses of the semantic item
                    List<String> SubClasses = request.getSubClasses();
                    //getting the superclasses of the semantic item
                    List<String> SuperClasses = request.getSuperClasses();
                    //get the support name
                    String SupportName = request.getSupportName();
                    //taking information about the primitives
                    //and convert it from a ros message in the Atoms class
                    Atoms object = new Atoms();
                    object.MapFromRosMsg(request.getObject());
                    System.out.println("input from semantic service"+object);
                    //converting from semantic to episodic
                    ArrayList<EpisodicPrimitive> Primitives = memory.fromSemanticToEpisodic(object, ontoRef);
                    System.out.println("After mapping "+Primitives);
                    // add objects
                    for (EpisodicPrimitive i : Primitives) {
                        for (EpisodicPrimitive j : Primitives)
                            if (!i.equals(j))
                                j.addDisjointIndividual(i.getInstance());
                        i.getObjectSemantics().clear(); // clean previus spatial relation
                        i.writeSemantic();
                        //System.out.println("Adding ith primitive"+i);
                        //i.saveOntology(EPISODIC_ONTO_FILE);

                    }

                    //adding the ObjectProperty
                    for (EpisodicPrimitive i : Primitives) {
                        i.ApplyRelations();
                        i.writeSemantic();
                        i.saveOntology(EPISODIC_ONTO_FILE);
                    }
                    //syncronize the reasoner
                    ontoRef.synchronizeReasoner();
                    //initialize the Episodic Item
                    EpisodicScene episodicScene = new EpisodicScene(Primitives, ontoRef, SceneName);
                    episodicScene.setSubClasses(SubClasses);
                    episodicScene.setSuperClasses(SuperClasses);
                    episodicScene.setSupportName(SupportName);
                    episodicScene.setAddingTime(true);
                    episodicScene.InitializeClasses(ontoRef);
                    episodicScene.InitializeSupport(ontoRef);
                    //checking whether learning the episodic item
                    System.out.println("checkin whether learning ...");
                    //if it should be learned
                    if (episodicScene.ShouldLearn(ontoRef)) {
                        //learn the episodic item
                        System.out.println("learning ... ");
                        episodicScene.Learn(ontoRef, memory.ComputeName(CLASS.SCENE,ontoRef));
                        //connect the primitive t the episodic items
                        for (EpisodicPrimitive i : Primitives) {
                            i.setSceneName(episodicScene.getEpisodicSceneName());
                            i.ApplySceneName();
                            i.writeSemantic();
                            ontoRef.synchronizeReasoner();
                            i.saveOntology(EPISODIC_ONTO_FILE);
                            //filling the response
                            response.setLearnt(true);
                        }
                        ontoRef.saveOntology(EPISODIC_ONTO_FILE);
                    } else {
                        //if the episodic item must not be learned
                        //removing the individual of geometric primitives
                        //removing the primitives from the ontology
                        for (EpisodicPrimitive i : Primitives) {
                            ontoRef.removeIndividual(i.getInstance());
                            ontoRef.synchronizeReasoner();
                        }
                        //filling the response
                        response.setLearnt(false);

                    }
                    //filling the response
                    response.setEpisodicSceneName(episodicScene.getEpisodicSceneName());



                }
                //retrieval
                else if (decision==2){
                    Set<String> forgotten = new HashSet<>();
                    //retrieval Semantic
                    //retrieved the episodic item linked to the semantic element retrieved
                    if(!request.getRetrievalSemantic().isEmpty()) {
                        //getting the semantic elements retrievel
                        List<String> individuals= memory.RetrievalSemanticEpisodic(request.getRetrievalSemantic(),ontoRef,forgotten);
                        individuals.removeAll(forgotten);
                        response.setRetrievalSemantic(individuals);
                    }
                    //retrieval Episodic
                    else {
                        //update the clock time
                        long time = System.currentTimeMillis();
                        MORFullIndividual clock = new MORFullIndividual(TIME.CLOCK,ontoRef);
                        clock.readSemantic();
                        clock.removeData(TIME.HAS_TIME_CLOCK);
                        clock.addData(TIME.HAS_TIME_CLOCK,time);
                        clock.writeSemantic();
                        ontoRef.synchronizeReasoner();
                        //taking information about the support
                        String support = request.getRetrieval().getSupport();
                        //taking the object property
                        List<sit_msgs.objectPropertyRetrieval> objectPropertyRetrievals = request.getRetrieval().getObjectProperty();
                        List<retrievalAtom> retrievalAtomsList = request.getRetrieval().getPrimitives();
                        // taking the time interval
                        String timeInterval = memory.timeIntervalClass(request.getRetrieval().getTime());
                        //Spatial Relationship
                        List<String> posssibleSceneSpatialRelationship = new ArrayList<>();
                        //Primitives
                        List<String> possiblePrimitiveScenes = new ArrayList<>();
                        //Support
                        List<String> possibleSupportScenes = new ArrayList<>();
                        //Time
                        List<String> possibleTimeIntervalScenes = new ArrayList<>();
                        //Final List
                        List<String> retrievedScenes = new ArrayList<>();
                        //if object property is not empty
                        if (!objectPropertyRetrievals.isEmpty()) {
                            posssibleSceneSpatialRelationship.addAll(
                                    memory.computePossibleSpatialRelationshipScene(objectPropertyRetrievals, ontoRef));
                            retrievedScenes = posssibleSceneSpatialRelationship;

                        }
                        //if the atoms informaton is not empty
                        if (!retrievalAtomsList.isEmpty()) {
                            possiblePrimitiveScenes.addAll(memory.computePossiblePrimitiveScenes(retrievalAtomsList, ontoRef));
                            retrievedScenes = possiblePrimitiveScenes;
                        }
                        //if the support is not empty
                        if (!support.isEmpty()) {
                            possibleSupportScenes.addAll(memory.computePossibleSupportScene(support, ontoRef));
                            retrievedScenes = possibleSupportScenes;
                        }
                        //if the time interval is not empty
                        if (!timeInterval.equals(TIME.NO_TIME)) {
                            possibleTimeIntervalScenes.addAll(memory.computePossibleTimeIntervalScenes(timeInterval, ontoRef));
                            retrievedScenes = possibleTimeIntervalScenes;
                        }

                        //FILLING THE RESPONSE
                        //make the list contain only the element common to all the required lists
                        if (!timeInterval.equals(TIME.NO_TIME)) {
                            retrievedScenes.retainAll(possibleTimeIntervalScenes);
                        }
                        if (!objectPropertyRetrievals.isEmpty()) {
                            retrievedScenes.retainAll(posssibleSceneSpatialRelationship);

                        }
                        if (!support.isEmpty()) {
                            retrievedScenes.retainAll(possibleSupportScenes);
                        }
                        if (!retrievalAtomsList.isEmpty()) {
                            retrievedScenes.retainAll(possiblePrimitiveScenes);
                        }
                        for (String s : retrievedScenes) {
                            MORFullIndividual ind = new MORFullIndividual(s, ontoRef);
                            ind.readSemantic();
                            if (ind.getLiteral(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT).parseBoolean()) {
                                forgotten.add(s);
                            }
                            memory.updateTimeEpisodic(s,ontoRef);

                        }
                        retrievedScenes.removeAll(forgotten);
                        response.setRetrievalEpisodic(retrievedScenes);
                    }
                        //updating the counter for retrieved forgot items
                        List<String> resetCounter= new ArrayList<>();
                        List<String> userNoForget= new ArrayList<>();
                        for (String s : forgotten){
                            memory.updateCounterRetrievalForgetting(resetCounter,userNoForget,s,ontoRef);
                        }
                        //removing user no forget from the ontology episodic
                        for (String s: userNoForget){
                            memory.removeUserNoForgetEpisodic(s,ontoRef,SITBase.EPISODIC_ONTO_FILE);
                        }
                        for (String s: resetCounter){
                            memory.removeUserNoForgetEpisodic(s,ontoRef,SITBase.EPISODIC_ONTO_FILE);
                        }
                        response.setResetCounter(resetCounter);
                        response.setUserNoForget(userNoForget);

                }

                //recognition
                else if (decision==4){
                    String SceneName = request.getSceneName();
                    String SupportName = request.getSupportName();
                    Atoms object = new Atoms();
                    object.MapFromRosMsg(request.getObject());
                    ArrayList<EpisodicPrimitive> Primitives = memory.fromSemanticToEpisodic(object, ontoRef);
                    // add objects
                    // add objects
                    for (EpisodicPrimitive i : Primitives) {
                        for (EpisodicPrimitive j : Primitives)
                            if (!i.equals(j))
                                j.addDisjointIndividual(i.getInstance());
                        i.getObjectSemantics().clear(); // clean previus spatial relation
                        i.writeSemantic();
                        //System.out.println("Adding ith primitive"+i);
                        //i.saveOntology(EPISODIC_ONTO_FILE);

                    }

                    //adding the ObjectProperty
                    for (EpisodicPrimitive i : Primitives) {
                        i.ApplyRelations();
                        i.writeSemantic();
                        i.saveOntology(EPISODIC_ONTO_FILE);
                    }
                    //syncronize the reasoner
                    ontoRef.synchronizeReasoner();
                    //initialize the scene
                    EpisodicScene episodicScene = new EpisodicScene(Primitives, ontoRef, SceneName);
                    episodicScene.setSupportName(SupportName);
                    episodicScene.setAddingTime(true);
                    episodicScene.InitializeClasses(ontoRef);
                    episodicScene.InitializeSupport(ontoRef);
                    System.out.println("CHECKING WHETHER LEARNING");
                    if(episodicScene.ShouldLearn(ontoRef)){
                        System.out.println("i have never actually seen this");

                    }
                   else {
                        MORFullIndividual clock = new MORFullIndividual(TIME.CLOCK,ontoRef);
                        clock.readSemantic();
                        clock.removeData(DATA_PROPERTY.TIME);
                        clock.addData(DATA_PROPERTY.TIME,System.currentTimeMillis());
                        clock.writeSemantic();
                        clock.saveOntology(EPISODIC_ONTO_FILE);
                        ontoRef.synchronizeReasoner();
                        //check whether it has been forgotten
                        MORFullIndividual ind= new MORFullIndividual(episodicScene.getEpisodicSceneName(),ontoRef);
                        ind.readSemantic();
                        memory.updateTimeEpisodic(ind);
                        if(!ind.getLiteral(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT).parseBoolean()) {
                            response.setEpisodicSceneName(episodicScene.getEpisodicSceneName());
                        }
                        else {
                            List<String> resetCounter= new ArrayList<>();
                            List<String> userNoForget= new ArrayList<>();
                            memory.updateCounterRetrievalForgetting(resetCounter,userNoForget,episodicScene.getEpisodicSceneName(),ontoRef);
                           for (String s : resetCounter) {
                               memory.removeUserNoForgetEpisodic(s,ontoRef,SITBase.EPISODIC_ONTO_FILE);
                           }
                           for (String s : userNoForget){
                               memory.removeUserNoForgetEpisodic(s,ontoRef,SITBase.EPISODIC_ONTO_FILE);
                           }
                           response.setResetCounter(resetCounter);
                           response.setUserNoForget(userNoForget);
                        }
                    }
                    //removing the individual of geometric primitives
                    for (EpisodicPrimitive i : Primitives) {
                        ontoRef.removeIndividual(i.getInstance());
                        ontoRef.synchronizeReasoner();
                    }
                }
                //forgetting
                else if (decision==3){
                    for (String s : request.getDeleteEpisodic()){
                        memory.deleteEpisodicItem(s,ontoRef);
                    }
                }
               // automatic forgetting
                else if (decision==0){
                    //forget and put  the forget Attribute.
                    ontoRef.synchronizeReasoner();
                    //put it as set in order to avoid repeated elements
                    Set<String> deleteElements= new HashSet<>();
                    deleteElements.addAll(request.getDeleteEpisodic());
                    //for all the elemnts that must be deleted
                    for(String s : deleteElements){
                        memory.deleteEpisodicItem(s,ontoRef);
                    }
                    //putting the flag to the forgot items
                    for(String s : request.getToBeForget()){
                        System.out.println("putting forget element"+s);
                        //declare the ontological individual
                        System.out.println("put forget for "+s); 
                        MORFullIndividual putForget= new MORFullIndividual(s,ontoRef);
                        //read the ontology
                        putForget.readSemantic();
                        //update the FORGOT data property
                        putForget.removeData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT);
                        putForget.addData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT,true,true);
                        //update the ontology
                        putForget.writeSemantic();
                        putForget.saveOntology(EPISODIC_ONTO_FILE);

                    }
                    for (String s : request.getDeleteSemantic()){
                       System.out.println("importante rimuovi gli elementi semantic : "+s);
                    }
                }

            }


        };



    }








}
