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
import it.emarolab.scene_identification_tagging.Interfaces.ROSSemanticInterface;
import it.emarolab.scene_identification_tagging.Score.ScoreJAVAInterface;

import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import it.emarolab.scene_identification_tagging.Interfaces.*;

/**
 * Ros service which manage the score ontology ;
 * Depending on the user decision it :
 * Intialize a new individuals in the score ontology
 * Update individual int the score ontology
 * Retrieved information about the current state of the score ontology itself and alowes the user either to
 * save or eliminate items.
 * Furthermore it automatically check, after each score update, the current state of the score ontology
 * And delete the elements which need to be forgot and comunicate such element to the client. <
 */
public class ScoreService extends  ROSSemanticInterface.ROSSemanticServer<ScoreInterfaceRequest, ScoreInterfaceResponse>
        implements SITBase, ScoreJAVAInterface, MemoryInterface {
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
                //getting from the request the name of the episodic item and semantic item
                EpisodicScoreItem episodic = request.getEpisodic();
                SemanticScoreItem semantic = request.getSemantic();
                //getting the information about the user Decision
                int decision = request.getDecision();
                //initiaizing the ontology reference
                OWLReferences ontoRef = getOntology();
                // suppress aMOR log
                it.emarolab.amor.owlDebugger.Logger.setPrintOnConsole(false);
                //MEMORIZATION
                if (decision == 1) {
                    //If there is a semantic item to be memorized
                    if (!semantic.getName().isEmpty()) {
                        //Definition of the semantic score item
                        SemanticScore semanticScore = new SemanticScore(semantic.getName(), semantic.getSubClasses(), semantic.getSuperClasses(), semantic.getFirstSuperClass(), semantic.getIsFirstSuperCLassOf(), ontoRef, true);
                        //Initialization of the semantic score item
                        semanticScore.semanticInitialization();
                    }
                    //If there is an episodic item to be memorized
                    if (!episodic.getName().isEmpty()) {
                        //definition of the episodic score item
                        EpisodicScore episodicScore = new EpisodicScore(episodic.getName(), episodic.getNameSemanticItem(), ontoRef, true);
                        //Initialization of the episodic item
                        episodicScore.episodicInitialization();

                    }
                }
                //RETRIEVAL
                else if (decision == 2 || decision==4) {
                    MORFullIndividual clock = new MORFullIndividual(TIME.CLOCK,ontoRef);
                    clock.readSemantic();
                    clock.removeData(SCORE.SCORE_PROP_HAS_TIME);
                    clock.addData(SCORE.SCORE_PROP_HAS_TIME,System.currentTimeMillis());
                    clock.writeSemantic();
                    clock.saveOntology(SCORE.SCORE_FILE_PATH);
                    ontoRef.synchronizeReasoner();
                    //if semantic retrieval has occured
                    if (!request.getSemanticRetrieval().isEmpty()) {
                        //for each element retireved
                        for (String s : request.getSemanticRetrieval()) {
                            if (!s.equals("owlNothing") && !s.equals("")) {
                                //defintiion of the semanticScore item
                                SemanticScore score = new SemanticScore(s, ontoRef);
                                //update of the semantic score item
                                score.semanticRetrieval();
                            }

                        }
                    }
                    //if episodic retrieval occured
                    if (!request.getEpisodicRetrieval().isEmpty()) {
                        for (String s : request.getEpisodicRetrieval()) {
                            if(!s.equals("")) {
                                //definition of the episodic score item
                                EpisodicScore score = new EpisodicScore(s, ontoRef);
                                //update of the score
                                score.episodicRetrieval();
                            }
                        }
                    }
                    //For all the element that needed to change user no forget property
                    for(String s : request.getUserNoForget()){
                        memory.resetCounter(s,ontoRef);
                        memory.changeUserNoForget(s,ontoRef,true);
                    }
                    //for all the element that needs to reset the counter
                    for (String s : request.getResetCounter()){
                        memory.resetCounter(s,ontoRef);
                    }

                    //the forgetting counter is done everytime the retrieval is finished
                    //definition of the forgetting item
                    Forgetting forgetting = new Forgetting(ontoRef);
                    //updating the counters
                    forgetting.updateTimes();
                    //updating the lists
                    forgetting.updateLists();
                    //deleting the element
                    forgetting.deleteEpisodic();
                    forgetting.deleteSemantic();
                    //filling the response
                    response.setDeleteEpisodic(forgetting.getForgotEpisodic());
                    response.setDeleteSemantic(forgetting.getForgotSemantic());
                    response.setPutForgotEpisodic(forgetting.getToBeForgottenEpisodic());
                    response.setPutForgotSemantic(forgetting.getToBeForgottenSemantic());

                    //saving the ontology
                    ontoRef.saveOntology(SCORE.SCORE_FILE_PATH);
                }
                //FORGETTING
                else if (decision == 3) {
                    //getting the user decision
                    int forgettingDecision = request.getDecisionForgetting();
                    if (forgettingDecision == 1) {
                        //preparing the output
                        ScoreCounterArray toBeForgettingSemantic= new ScoreCounterArray();
                        ScoreCounterArray toBeForgettingEpisodic= new ScoreCounterArray();
                        ScoreCounterArray forgotEpisodic = new ScoreCounterArray();
                        ScoreCounterArray forgotSemantic= new ScoreCounterArray();
                        ScoreCounterArray lowScoreSemantic= new ScoreCounterArray();
                        ScoreCounterArray lowScoreEpisodic= new ScoreCounterArray();
                        //definition of the forgetting item
                        Forgetting forgetting = new Forgetting(ontoRef);
                        //preparing the output
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
                        //filling the response
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
                            memory.resetCounter(s,ontoRef);
                            memory.changeUserNoForget(s,ontoRef,true);
                        }

                    }
                    else if (forgettingDecision == 4){
                        //removing the save item
                        for (String s : request.getUserRemoveNoForget()){
                            memory.changeUserNoForget(s,ontoRef,false);

                        }
                    }
                }
            }


        };


    }

    public ScoreCounter createToBeForgettingItem(String name, OWLReferences ontoRef, String NameTimesProperty){
        ScoreCounter score = new ScoreCounter(name);
        MORFullIndividual scoreInd= new MORFullIndividual(name, ontoRef);
        scoreInd.readSemantic();
        if(!NameTimesProperty.equals(SCORE.SCORE_PROP_TIMES_FORGOTTEN)) {
            score.setCounter(scoreInd.getLiteral(NameTimesProperty).parseInteger());
        }
        score.setScoreValue(scoreInd.getLiteral(SCORE.SCORE_PROP_HAS_SCORE).parseFloat());
        return score;
    }

}

