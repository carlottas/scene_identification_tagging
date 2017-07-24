package it.emarolab.scene_identification_tagging.Services;

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
                OWLReferences ontoRef = getOntology();
                // suppress aMOR log
                it.emarolab.amor.owlDebugger.Logger.setPrintOnConsole(false);
                int decision = request.getDecision();
                if (decision == 1) {
                    String SceneName = request.getSceneName();
                    List<String> SubClasses = request.getSubClasses();
                    List<String> SuperClasses = request.getSuperClasses();
                    String SupportName = request.getSupportName();
                    Atoms object = new Atoms();
                    object.MapFromRosMsg(request.getObject());
                    ArrayList<EpisodicPrimitive> Primitives = memory.fromSemanticToEpisodic(object, ontoRef);
                    // add objects
                    for (EpisodicPrimitive i : Primitives) {
                        for (EpisodicPrimitive j : Primitives)
                            if (!i.equals(j))
                                j.addDisjointIndividual(i.getInstance());
                        i.getObjectSemantics().clear(); // clean previus spatial relation
                        i.writeSemantic();
                    }
                    //adding the ObjectProperty
                    for (EpisodicPrimitive i : Primitives) {
                        i.ApplyRelations();
                        i.writeSemantic();
                        //i.saveOntology(EPISODIC_ONTO_FILE);
                    }
                    ontoRef.synchronizeReasoner();
                    //initialize the scene
                    EpisodicScene episodicScene = new EpisodicScene(Primitives, ontoRef, SceneName);
                    episodicScene.setSubClasses(SubClasses);
                    episodicScene.setSuperClasses(SuperClasses);
                    episodicScene.setSupportName(SupportName);
                    episodicScene.setAddingTime(true);
                    episodicScene.InitializeClasses(ontoRef);
                    episodicScene.InitializeSupport(ontoRef);
                    System.out.println("CHECKING WHETHER LEARNING");
                    if (episodicScene.ShouldLearn(ontoRef)) {
                        episodicScene.Learn(ontoRef, memory.ComputeName(CLASS.SCENE,ontoRef));
                        for (EpisodicPrimitive i : Primitives) {
                            i.setSceneName(episodicScene.getEpisodicSceneName());
                            i.ApplySceneName();
                            i.writeSemantic();
                            i.saveOntology(EPISODIC_ONTO_FILE);
                            response.setLearnt(true);
                        }
                    } else {
                        //removing the individual of geometric primitives
                        for (EpisodicPrimitive i : Primitives) {
                            ontoRef.removeIndividual(i.getInstance());
                            ontoRef.synchronizeReasoner();
                        }
                        response.setLearnt(false);

                    }
                    //filling the response
                    response.setEpisodicSceneName(episodicScene.getEpisodicSceneName());


                }
                //retrieval
                else if (decision==2){
                    //retrieval Semantic
                    if(!request.getRetrievalSemantic().isEmpty()) {
                        List<String> classes = request.getRetrievalSemantic();
                        List<String> individuals = new ArrayList<>();
                        Set<String> forgotten = new HashSet<>();
                        for (String s : classes) {
                            if (!s.equals("owlNothing")) {
                                SceneClassDescriptor currentClass = new SceneClassDescriptor(s, ontoRef);
                                currentClass.readSemantic();
                                MORAxioms.Individuals i = currentClass.getIndividualClassified();
                                for (OWLNamedIndividual ind:i){
                                    MORFullIndividual individual= new MORFullIndividual(ind,ontoRef);
                                    individual.readSemantic();
                                    if(individual.getLiteral(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT).parseBoolean()) {
                                        //todo Change with ground
                                        forgotten.add(ind.getIRI().toString().substring(EPISODIC_ONTO_IRI.length() + 1));
                                    }
                                    individuals.add(ind.getIRI().toString().substring(EPISODIC_ONTO_IRI.length()+1));
                                }
                            }
                        }

                        individuals.removeAll(forgotten);
                        response.setRetrievalSemantic(individuals);
                    }
                    //retrieval Episodic
                    else {
                        //taking information about the support
                        String support = request.getRetrieval().getSupport();
                        //taking the object property
                        List<sit_msgs.objectPropertyRetrieval> objectPropertyRetrievals = request.getRetrieval().getObjectProperty();
                        List<retrievalAtom> retrievalAtomsList= request.getRetrieval().getPrimitives();
                        // taking the time interval
                        String timeInterval = memory.timeIntervalClass(request.getRetrieval().getTime());
                        //Spatial Relationship
                        List<String> posssibleSceneSpatialRelationship = new ArrayList<>();
                        //Primitives
                        List<String> possiblePrimitiveScenes = new ArrayList<>();
                        //Support
                        List<String> possibleSupportScenes= new ArrayList<>();
                        //Time
                        List<String> possibleTimeIntervalScenes= new ArrayList<>();
                        //Final List
                        List<String> retrievedScenes= new ArrayList<>();
                        //if object property is not empty
                        if (!objectPropertyRetrievals.isEmpty()) {
                            posssibleSceneSpatialRelationship.addAll(
                                    memory.computePossibleSpatialRelationshipScene(objectPropertyRetrievals,ontoRef));
                            retrievedScenes=posssibleSceneSpatialRelationship;

                        }
                        if(! retrievalAtomsList.isEmpty()){
                            possiblePrimitiveScenes.addAll(memory.computePossiblePrimitiveScenes(retrievalAtomsList, ontoRef));
                            retrievedScenes=possiblePrimitiveScenes;
                        }

                        if(!support.isEmpty()){
                            possibleSupportScenes.addAll(memory.computePossibleSupportScene(support,ontoRef));
                            retrievedScenes=possibleSupportScenes;
                        }

                        if(!timeInterval.equals(TIME.NO_TIME)){
                            possibleTimeIntervalScenes.addAll(memory.computePossibleTimeIntervalScenes(timeInterval,ontoRef));
                            retrievedScenes=possibleTimeIntervalScenes;
                        }
                        ///////FILLING THE RESPONSE
                        //make the list contain only the element common to all the required lists
                        if(!timeInterval.equals(TIME.NO_TIME)){
                            retrievedScenes.retainAll(possibleTimeIntervalScenes);
                        }
                        if(!objectPropertyRetrievals.isEmpty()){
                            retrievedScenes.retainAll(posssibleSceneSpatialRelationship);

                        }
                        if(!support.isEmpty()){
                            retrievedScenes.retainAll(possibleSupportScenes);
                        }
                        if(!retrievalAtomsList.isEmpty()){
                            retrievedScenes.retainAll(possiblePrimitiveScenes);
                        }
                        Set<String> forgotten = new HashSet<>();
                        for (String s : retrievedScenes){
                            MORFullIndividual ind= new MORFullIndividual(s,ontoRef);
                            ind.readSemantic();
                            if(ind.getLiteral(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT).parseBoolean()){
                                forgotten.add(s);
                            }

                        }
                        retrievedScenes.removeAll(forgotten);
                        response.setRetrievalEpisodic(retrievedScenes);
                        List<String> resetCounter= new ArrayList<>();
                        List<String> userNoForget= new ArrayList<>();
                        for (String s : forgotten){
                            MORFullIndividual ind = new MORFullIndividual(s,ontoRef);
                            ind.readSemantic();
                            System.out.println(ind);
                            float counter = ind.getLiteral(FORGETTING.NAME_DATA_PROPERTY_RETRIEVAL_FORGOT).parseFloat();
                            float newCounter = 0;
                            if (counter<1){
                                newCounter=counter+FORGETTING.INCREMENT_ONE;
                                if (newCounter>=1){
                                    resetCounter.add(s);

                                }

                            }
                            else if (counter<2 && counter>=1){
                                newCounter=counter+FORGETTING.INCREMENT_TWO;
                                if(newCounter>=2){
                                    resetCounter.add(s);
                                }

                            }
                            else if (counter<3&&counter>=2){
                                newCounter=counter+FORGETTING.INCREMENT_THREE;
                                if(newCounter>=3){
                                    userNoForget.add(s);
                                }
                            }
                            ind.removeData(FORGETTING.NAME_DATA_PROPERTY_RETRIEVAL_FORGOT);
                            ind.addData(FORGETTING.NAME_DATA_PROPERTY_RETRIEVAL_FORGOT,newCounter);
                            ind.writeSemantic();
                            ind.saveOntology(EPISODIC_ONTO_FILE);
                        }
                        //removing user no forget from the ontology episodic
                        for (String s: userNoForget){
                            MORFullIndividual ind = new MORFullIndividual(s,ontoRef);
                            ind.readSemantic();
                            ind.removeData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT);
                            ind.addData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT,false,true);
                            ind.writeSemantic();
                            ind.saveOntology(EPISODIC_ONTO_FILE);
                        }
                        for (String s: resetCounter){
                            MORFullIndividual ind = new MORFullIndividual(s,ontoRef);
                            ind.readSemantic();
                            ind.removeData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT);
                            ind.addData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT,false,true);
                            ind.writeSemantic();
                            ind.saveOntology(EPISODIC_ONTO_FILE);
                        }
                        response.setResetCounter(resetCounter);
                        response.setUserNoForget(userNoForget);

                    }



                }
                //forgetting
                else if (decision==3){

                }
                else if (decision==4){
                    String SceneName = request.getSceneName();
                    String SupportName = request.getSupportName();
                    Atoms object = new Atoms();
                    object.MapFromRosMsg(request.getObject());
                    ArrayList<EpisodicPrimitive> Primitives = memory.fromSemanticToEpisodic(object, ontoRef);
                    // add objects
                    for (EpisodicPrimitive i : Primitives) {
                        for (EpisodicPrimitive j : Primitives)
                            if (!i.equals(j))
                                j.addDisjointIndividual(i.getInstance());
                        i.getObjectSemantics().clear(); // clean previus spatial relation
                        i.writeSemantic();
                    }
                    //adding the ObjectProperty
                    for (EpisodicPrimitive i : Primitives) {
                        i.ApplyRelations();
                        i.writeSemantic();
                        //i.saveOntology(EPISODIC_ONTO_FILE);
                    }
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
                        //check whether it has been forgotten

                        MORFullIndividual ind= new MORFullIndividual(episodicScene.getEpisodicSceneName(),ontoRef);
                        ind.readSemantic();
                        if(!ind.getLiteral(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT).parseBoolean()) {
                            response.setEpisodicSceneName(episodicScene.getEpisodicSceneName());
                        }
                        else {
                            float counter = ind.getLiteral(FORGETTING.NAME_DATA_PROPERTY_RETRIEVAL_FORGOT).parseFloat();
                            float newCounter = 0;
                            if (counter<1){
                                newCounter=counter+FORGETTING.INCREMENT_ONE;
                                if (newCounter>=1){
                                    List<String> resetCounter= new ArrayList<>();
                                    resetCounter.add(episodicScene.getEpisodicSceneName());
                                    MORFullIndividual episodicSceneIndividual = new MORFullIndividual(episodicScene.getEpisodicSceneName(),ontoRef);
                                    episodicSceneIndividual.readSemantic();
                                    episodicSceneIndividual.removeData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT);
                                    episodicSceneIndividual.addData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT,false,true);
                                    episodicSceneIndividual.writeSemantic();
                                    episodicSceneIndividual.saveOntology(EPISODIC_ONTO_FILE);
                                    response.setResetCounter(resetCounter);

                                }

                            }
                            else if (counter<2 && counter>=1){
                                newCounter=counter+FORGETTING.INCREMENT_TWO;
                                if(newCounter>=2){
                                    List<String> resetCounter= new ArrayList<>();
                                    resetCounter.add(episodicScene.getEpisodicSceneName());
                                    MORFullIndividual episodicSceneIndividual = new MORFullIndividual(episodicScene.getEpisodicSceneName(),ontoRef);
                                    episodicSceneIndividual.readSemantic();
                                    episodicSceneIndividual.removeData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT);
                                    episodicSceneIndividual.addData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT,false,true);
                                    episodicSceneIndividual.writeSemantic();
                                    episodicSceneIndividual.saveOntology(EPISODIC_ONTO_FILE);
                                    response.setResetCounter(resetCounter);
                                }

                            }
                            else if (counter<3&&counter>=2){
                                newCounter=counter+FORGETTING.INCREMENT_THREE;
                                if(newCounter>=3){
                                    List<String> userNoForget= new ArrayList<>();
                                    userNoForget.add(episodicScene.getEpisodicSceneName());
                                    MORFullIndividual episodicSceneIndividual = new MORFullIndividual(episodicScene.getEpisodicSceneName(),ontoRef);
                                    episodicSceneIndividual.readSemantic();
                                    episodicSceneIndividual.removeData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT);
                                    episodicSceneIndividual.addData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT,false,true);
                                    episodicSceneIndividual.writeSemantic();
                                    episodicSceneIndividual.saveOntology(EPISODIC_ONTO_FILE);
                                    response.setUserNoForget(userNoForget);


                                }
                            }
                            ind.removeData(FORGETTING.NAME_DATA_PROPERTY_RETRIEVAL_FORGOT);
                            ind.addData(FORGETTING.NAME_DATA_PROPERTY_RETRIEVAL_FORGOT,newCounter);
                            ind.writeSemantic();
                            ind.saveOntology(EPISODIC_ONTO_FILE);

                        }
                    }
                    //removing the individual of geometric primitives
                    for (EpisodicPrimitive i : Primitives) {
                        ontoRef.removeIndividual(i.getInstance());
                        ontoRef.synchronizeReasoner();
                    }



                }

                else if (decision==0){
                    //forget and put  the forget Attribute.
                    ontoRef.synchronizeReasoner();
                    //put it as set in order to avoid repeated elements
                    Set<String> deleteElements= new HashSet<>();
                    deleteElements.addAll(request.getDeleteEpisodic());
                    for(String s : deleteElements){
                        MORFullIndividual delete= new MORFullIndividual(s,ontoRef);
                        delete.readSemantic();
                        List<String> primitivesDelete= new ArrayList<>();
                        memory.objectPropertyValues(delete.getObjectSemantics(),OBJECT_PROPERTY.HAS_SCENE_PRIMITIVE,primitivesDelete,EPISODIC_ONTO_IRI);
                        ontoRef.removeIndividual(s);
                        for(String i:primitivesDelete){
                            ontoRef.removeIndividual(i);
                        }
                        ontoRef.saveOntology(EPISODIC_ONTO_FILE);
                    }
                    for(String s : request.getToBeForget()){
                        System.out.println("putting to be forgotten to element"+s);
                        MORFullIndividual putForget= new MORFullIndividual(s,ontoRef);
                        putForget.readSemantic();
                        putForget.removeData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT);
                        putForget.addData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT,true,true);
                        putForget.writeSemantic();
                        putForget.saveOntology(EPISODIC_ONTO_FILE);

                    }
                    for (String s : request.getDeleteSemantic()){
                        //delete
                    }
                }

            }


        };



    }








}
