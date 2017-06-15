
package it.emarolab.scene_identification_tagging;


import it.emarolab.scene_identification_tagging.realObject.Cylinder;
import it.emarolab.scene_identification_tagging.realObject.Sphere;
import javafx.scene.shape.*;
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
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Vector;
import java.util.Arrays;

public class SemanticService
        extends AbstractNodeMain
    implements SITBase
{    private static final String SERVICE_NAME="EpisodicService";
     private static final String SPHERE="Sphere";
     private static final String PLANE="Plane";
     private static final String CYLINDER="Cyilinder";
     private static final String CONE="Cone";
    private static final String ONTO_NAME = "ONTO_NAME"; // an arbritary name to refer the ontology

    public boolean initParam(ConnectedNode node) {

        // stat the service
        node.newServiceServer(
                getServerName(), // set service name
                SemanticInterface._TYPE, // set ROS service message
                getService(node) // set ROS service response
        );
        return true;
    }


    public String getServerName() {
        return SERVICE_NAME;
    }
    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of( getServerName());
    }
    @Override
    public void onStart(ConnectedNode node){
        super.onStart( node);
        // get ROS parameter
        if( ! initParam( node))
            System.exit( 1);
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
                OWLReferences ontoRef = OWLReferencesInterface.OWLReferencesContainer.newOWLReferenceFromFileWithPellet(
                        ONTO_NAME, ONTO_FILE, ONTO_IRI, true);

                // suppress aMOR log
                it.emarolab.amor.owlDebugger.Logger.setPrintOnConsole( false);

                // initialise objects
                Set< GeometricPrimitive> objects = new HashSet<>();
                List<atom> geometricPrimitives=request.getGeometricPrimitives();
                for (atom g:geometricPrimitives) {
                    //TODO  find a way to have a vector, list instead of an array
                    float[] coefficient = g.getCoefficients();

                    if (SPHERE == g.getLabel()) {
                        //TODO warning if the coefficinets are not the right number
                        if(coefficient.length==4) {
                        Sphere s = new Sphere(ontoRef);
                        s.shouldAddTime(true);
                        s.setCenter(coefficient[0], coefficient[1], coefficient[2]);
                        s.setRadius(coefficient[3]);
                        objects.add(s);
                        }
                        else
                        {
                            System.out.println("Wrong coefficients for sphere!");
                        }

                    } else if (PLANE == g.getLabel()) {
                        //to do check size =7
                        if(coefficient.length==7) {
                            Plane p = new Plane(ontoRef);
                            p.shouldAddTime(true);
                            p.setAxis(coefficient[0], coefficient[1], coefficient[2]);
                            p.setCenter(coefficient[3], coefficient[4], coefficient[5]);
                            p.setHessian(coefficient[6]);
                            objects.add(p);
                        }
                        else
                        {
                         System.out.println("Wrong coefficient for Plane ");
                        }

                    } else if (CYLINDER == g.getLabel()) {
                        if(coefficient.length==11) {
                            Cylinder c = new Cylinder(ontoRef);
                            c.shouldAddTime(true);
                            c.setCenter(coefficient[0], coefficient[1], coefficient[2]);
                            c.setAxis(coefficient[3], coefficient[4], coefficient[5]);
                            c.setApex(coefficient[6], coefficient[7], coefficient[8]);
                            c.setRadius(coefficient[9]);
                            c.setHeight(coefficient[10]);
                            objects.add(c);
                        }
                        else{
                            System.out.println("Wrong coefficient for Cylinder");
                        }

                    } else if (CONE == g.getLabel()) {
                        if(coefficient.length==11) {
                            Cone c = new Cone(ontoRef);
                            c.shouldAddTime(true);
                            c.setCenter(coefficient[0], coefficient[1], coefficient[2]);
                            c.setAxis(coefficient[3], coefficient[4], coefficient[5]);
                            c.setApex(coefficient[6], coefficient[7], coefficient[8]);
                            c.setRadius(coefficient[9]);
                            c.setHeight(coefficient[10]);
                            objects.add(c);
                        }
                        else{
                            System.out.println("Wrong coefficient for Cone");
                        }

                    } else {
                        System.out.println("Unknwonw label");
                    }
                }

                // add objects
                for ( GeometricPrimitive i : objects){
                    for ( GeometricPrimitive j : objects)
                        if (!i.equals(j))
                            j.addDisjointIndividual( i.getInstance());
                    i.getObjectSemantics().clear(); // clean previus spatial relation
                    i.writeSemantic();
                }
                // run SWRL
                ontoRef.synchronizeReasoner();
                // get SWRL results
                //should get the semantic
                for ( GeometricPrimitive i : objects) {
                    // it could be implemented faster
                    i.readSemantic();
                }

                // create scene and reason for recognition
                SceneRepresentation recognition1 = new SceneRepresentation( objects, ontoRef);
                System.out.println( "Recognised with best confidence: " + recognition1.getRecognitionConfidence() + " should learn? " + recognition1.shouldLearn());
                System.out.println( "Best recognised class: " + recognition1.getBestRecognitionDescriptor());
                System.out.println( "Other recognised classes: " + recognition1.getSceneDescriptor().getTypeIndividual());

                // learn the new scene if is the case
                if ( recognition1.shouldLearn()) {
                    System.out.println("Learning.... ");
                    //TODO should read in the ontology the parameter to know the number of the scene and then SceneNumber
                    recognition1.learn("TestScene");
                }
                // clean ontology
                ontoRef.removeIndividual( recognition1.getSceneDescriptor().getInstance());
                for ( GeometricPrimitive i : objects)
                    ontoRef.removeIndividual( i.getInstance());
                ontoRef.synchronizeReasoner();
                //take class name
                ArrayList<String> subClasses=new ArrayList<String>();
                ArrayList<String> superClasses=new ArrayList<String>();
                String Name= recognition1.getBestRecognitionDescriptor().getInstance().getClassExpressionType().getName();
                //t super classes
                for (OWLClass i: recognition1.getBestRecognitionDescriptor().getSubConcept()){
                     subClasses.add(i.getClassExpressionType().getName());
                }
                //take sub classes
                for (OWLClass i: recognition1.getBestRecognitionDescriptor().getSuperConcept()){
                    superClasses.add(i.getClassExpressionType().getName());
                }

                recognition1.getSceneDescriptor().


            }




        };


    }


}

