
package it.emarolab.scene_identification_tagging.Interfaces;

import it.emarolab.amor.owlDebugger.Logger;
import it.emarolab.amor.owlInterface.OWLReferences;
import it.emarolab.amor.owlInterface.OWLReferencesInterface;
import org.ros.internal.message.Message;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.parameter.ParameterTree;
import org.ros.node.service.ServiceResponseBuilder;

/**
 * The container for ROS-OWL service usage facilities.
 * <p>
 *     It contains:<ul>
 *        <li> {@link ROSTest} to manage files,
 *        <li> {@link ROSServer}, to manage ROS-Java services
 *        <li> {@link ROSSemanticServer}, to manage a ROS services that has an ontology and can manage test files.
 *     </ul>
 *
 * <div style="text-align:center;"><small>
 * <b>File</b>:        it.emarolab.sit_test_srv_amor.srvInterface.ROSSemanticInterface <br>
 * <b>Licence</b>:     GNU GENERAL PUBLIC LICENSE. Version 3, 29 June 2007 <br>
 * <b>Author</b>:      Buoncompagni Luca (luca.buoncompagni@edu.unige.it) <br>
 * <b>affiliation</b>: DIBRIS, EMAROLab, University of Genoa. <br>
 * <b>date</b>:        09/04/17 <br>
 * </small></div>
 *
 *
 */
public interface ROSSemanticInterface {


    /**
     * An interface for a ROS-Java services that operates on a OWL ontology.
     * <p>
     *     This class uses the file conventions based by {@link ROSTest} to
     *     {@link #loadSemantics()} from ROS parameters. Moreover, it
     *     implements the operation performed during service call through:
     *     {@link #getService(ConnectedNode)}.<br>
     *     This type of service listen for ROS parameter only during start-up.
     *     (i.e.: restart the server for changes).
     *
     * <div style="text-align:center;"><small>
     * <b>File</b>:        it.emarolab.sit_test_srv_amor.srvInterface.ROSSemanticInterface <br>
     * <b>Licence</b>:     GNU GENERAL PUBLIC LICENSE. Version 3, 29 June 2007 <br>
     * <b>Author</b>:      Buoncompagni Luca (luca.buoncompagni@edu.unige.it) <br>
     * <b>affiliation</b>: DIBRIS, EMAROLab, University of Genoa. <br>
     * <b>date</b>:        09/04/17 <br>
     * </small></div>
     *
     * @see SIT_TEST_DEFAULTS
     * @see ROSTest
     * @see ROSServer
     */
    abstract class ROSSemanticServer<I extends Message, O extends Message>
            extends ROSServer<I,O>
           {

        // loaded from abstract interface
        private OWLReferences ontology;

        /**
         * This functions generates a copy of the A-box representation for the
         * knowledge of this server from {@link #getAboxPath()} and saves it on
         * {@link #getOntologyPath()}. Than, it instanciate a working references
         * to the {@link #getOntology()}.
         */
        protected void loadSemantics(String ontoName, String FilePath,String IRI){
            /*

            // load abox
            OWLReferences ontoRef = OWLReferencesInterface.OWLReferencesContainer.newOWLReferenceFromFileWithPellet(
                    ontoName,
                    FilePath,
                    IRI,
                  true
            );
            */
            // reinstanciate OWL reference
            //OWLReferencesInterface.OWLReferencesContainer.getOWLReferencesKeys().remove( ontoName);
            ontology = OWLReferencesInterface.OWLReferencesContainer.newOWLReferenceFromFileWithPellet(
                    ontoName,
                    FilePath,
                    IRI,
                    true
            );
            //ontology.setOWLManipulatorBuffering(true);
        }

        /**
         * @return the ontology used by the server, loaded during initialisation
         * from path and IRI given from ROS parameters.
         */
        public OWLReferences getOntology() {
            return ontology;
        }
    }

    /**
     * An interface for ROS-Java services.
     * <p>
     *     This is an easy wrapper of the {@link AbstractNodeMain} class
     *     that implements a ROS server that need input parameter for initialising
     *     a data structure.<br>
     *     Note (only) here there is the ROS message interface.
     *
     * <div style="text-align:center;"><small>
     * <b>File</b>:        it.emarolab.sit_test_srv_amor.srvInterface.ROSSemanticInterface <br>
     * <b>Licence</b>:     GNU GENERAL PUBLIC LICENSE. Version 3, 29 June 2007 <br>
     * <b>Author</b>:      Buoncompagni Luca (luca.buoncompagni@edu.unige.it) <br>
     * <b>affiliation</b>: DIBRIS, EMAROLab, University of Genoa. <br>
     * <b>date</b>:        09/04/17 <br>
     * </small></div>
     *

     * @see ROSSemanticServer
     */
    abstract class ROSServer<I,O>
            extends AbstractNodeMain {

        /**
         * @return the name of the implemented ROS service.
         */
        abstract public String getServerName();
        /**
         * Implements the server steps to be performed during service call.
         * @param node the bridge to the standard ROS service
         * @return the object that perform computation during service call.
         */
        abstract public ServiceResponseBuilder<I, O> getService( ConnectedNode node);
        /**
         * Initialises all the parameter used from the server.
         * @param node the bridge to the standard ROS service
         * @return {@code true} if the initialisation is successful. {@code False} otherwise.
         */
        abstract public boolean initParam( ConnectedNode node);

        /**
         * It is based on {@link #getServerName()}.
         * @return the default ROS node name.
         */
        @Override
        public GraphName getDefaultNodeName() {
            return GraphName.of( getServerName());
        }

        /**
         * Called during server startup by ROSJava, it initialise all
         * the required field based on ROS parameter
         * by calling {@link #initParam(ConnectedNode)}.
         * This method, does not set the steps that the server should perform at any call.
         * To do so, use {@link #getService(ConnectedNode)} within
         * {@code node.newServiceServer( ... )}.
         * @param node the ROS node from which get the parameters.
         */
        @Override
        public void onStart(ConnectedNode node){
            super.onStart( node);
            // get ROS parameter
            if( ! initParam( node))
                System.exit( 1);
        }
    }
}
