
package it.emarolab.scene_identification_tagging;


import it.emarolab.scene_identification_tagging.realObject.Cylinder;
import it.emarolab.scene_identification_tagging.realObject.Sphere;
import it.emarolab.scene_identification_tagging.sceneRepresentation.SpatialRelation;
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
    implements SITBase {
    private static final String SERVICE_NAME = "SemanticService";
    private static final String SPHERE = "Sphere";
    private static final String PLANE = "Plane";
    private static final String CYLINDER = "Cyilinder";
    private static final String CONE = "Cone";
    private static final String ONTO_NAME = "ONTO_NAME"; // an arbritary name to refer the ontology
    private static final String NAME = "testScene";
    private static final String PROP_LEFT="isLeftOf";
    private static final String PROP_RIGHT="isRightOf";
    private static final String PROP_PERPENDICULAR="isPerpendicularTo";
    private static final String PROP_PARALLEL="isParallelTo";
    private static final String PROP_IS_IN_FRONT_OF="isInFrontOf";
    private static final String PROP_IS_BEHIND_OF="isBehindOf";
    private static final String PROP_IS_BELOW_OF="isBelowOf";
    private static final String PROP_IS_ABOVE_OF="isAboveOf";
    private static final String PROP_IS_ALONG_X="isAlongX";
    private static final String PROP_IS_ALONG_Y="isAlongY";
    private static final String PROP_IS_ALONG_Z="isAlongZ";
    private static final String PROP_IS_COAXIAL_WITH="isCoaxialWith";
    private static final String PROP_HAS_RADIUS="hasRadius";

    String YELLOW="Yellow";
    String BLUE="Blue";
    String GREEN="Green";
    String PINK="Pink";
    String RED="Red";

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
                OWLReferences ontoRef = OWLReferencesInterface.OWLReferencesContainer.newOWLReferenceFromFileWithPellet(
                        ONTO_NAME, ONTO_FILE, ONTO_IRI, true);

                // suppress aMOR log
                it.emarolab.amor.owlDebugger.Logger.setPrintOnConsole(false);

                // initialise objects
                Set<GeometricPrimitive> objects = new HashSet<>();
                List<atom> geometricPrimitives = request.getGeometricPrimitives();
                for (atom g : geometricPrimitives) {
                    System.out.println("for all the geometric primitives received");
                    //TODO  find a way to have a vector, list instead of an array
                    float[] coefficient = g.getCoefficients();
                    System.out.println(g.getLabel());
                    if (SPHERE.equals(g.getLabel())) {
                        System.out.println("if it is a Sphere");
                        //TODO warning if the coefficinets are not the right number
                        if (coefficient.length == 4) {
                            System.out.println("correct number of coefficient");
                            Sphere s = new Sphere(ontoRef);
                            s.shouldAddTime(true);
                            s.setCenter(coefficient[0], coefficient[1], coefficient[2]);
                            s.setRadius(coefficient[3]);
                            objects.add(s);
                        } else {
                            System.out.println("Wrong coefficients for sphere!");
                        }

                    } else if (PLANE.equals(g.getLabel())) {
                        System.out.println("if it is a plane");
                        if (coefficient.length == 7) {
                            System.out.println("correct number of coefficients");
                            Plane p = new Plane(ontoRef);
                            p.shouldAddTime(true);
                            p.setAxis(coefficient[0], coefficient[1], coefficient[2]);
                            p.setCenter(coefficient[3], coefficient[4], coefficient[5]);
                            p.setHessian(coefficient[6]);
                            objects.add(p);
                        } else {
                            System.out.println("Wrong coefficient for Plane ");
                        }

                    } else if (CYLINDER.equals(g.getLabel())) {
                        if (coefficient.length == 11) {
                            Cylinder c = new Cylinder(ontoRef);
                            c.shouldAddTime(true);
                            c.setCenter(coefficient[0], coefficient[1], coefficient[2]);
                            c.setAxis(coefficient[3], coefficient[4], coefficient[5]);
                            c.setApex(coefficient[6], coefficient[7], coefficient[8]);
                            c.setRadius(coefficient[9]);
                            c.setHeight(coefficient[10]);
                            objects.add(c);
                        } else {
                            System.out.println("Wrong coefficient for Cylinder");
                        }

                    } else if (CONE.equals(g.getLabel())) {
                        if (coefficient.length == 11) {
                            Cone c = new Cone(ontoRef);
                            c.shouldAddTime(true);
                            c.setCenter(coefficient[0], coefficient[1], coefficient[2]);
                            c.setAxis(coefficient[3], coefficient[4], coefficient[5]);
                            c.setApex(coefficient[6], coefficient[7], coefficient[8]);
                            c.setRadius(coefficient[9]);
                            c.setHeight(coefficient[10]);
                            objects.add(c);
                        } else {
                            System.out.println("Wrong coefficient for Cone");
                        }

                    } else {
                        System.out.println("Unknwonw label");
                    }
                }

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
                    System.out.println("Learning.... ");
                    //TODO change the name
                    recognition1.learn(NAME);
                }

                // clean ontology
                ontoRef.removeIndividual(recognition1.getSceneDescriptor().getInstance());
                for (GeometricPrimitive i : objects)
                    ontoRef.removeIndividual(i.getInstance());
                ontoRef.synchronizeReasoner();
                ontoRef.saveOntology();

                System.out.println("saving the ontology");
                recognition1.getBestRecognitionDescriptor().saveOntology(ONTO_FILE);
                //take class name
                response.setSceneName(recognition1.getBestRecognitionDescriptor().NameToString(ONTO_NAME.length()+1));
                response.setSubClasses(recognition1.getBestRecognitionDescriptor().SubConceptToString());
                response.setSuperClasses(recognition1.getBestRecognitionDescriptor().SuperConceptToString());
                List<atom> primitivesOutput=new ArrayList<atom>();
                for (GeometricPrimitive i : objects) {
                    // it could be implemented faster
                    atom g;
                    List<Float> coefficients=new ArrayList<>();
                    i.readSemantic();
                    //check how the name is given
                    g.setName(i.getGround().toString());
                    //check again how the name is given
                    g.setLabel(i.getTypeIndividual().toString());
                    if(g.getLabel().equals(SPHERE)){
                        coefficients.add(ValueOfDataPropertyFloat(i.getDataSemantics(),PROP_HAS_RADIUS));
                        //so on

                    }
                    List<spatialRelationship> sr=new ArrayList<spatialRelationship>();
                    g.setRelations(computeSR(i));

                }
                //fill the response
                //DONE





            }







        };


    }

    public List<spatialRelationship> computeSR(GeometricPrimitive subject){
        //solve how to initialize
        //fo it for all the properties
        subject.readSemantic();
        List<String> Individuals1 = new ArrayList<String>();
        List<spatialRelationship> sr=new ArrayList<spatialRelationship>();
        objectProperty(subject.getObjectSemantics(),PROP_IS_ABOVE_OF,Individuals1);
        if(!Individuals1.isEmpty()){
            //add to sr
        }
        List<String> Individuals2 = new ArrayList<String>();
        objectProperty(subject.getObjectSemantics(),PROP_IS_ALONG_X,Individuals2);
        if(!Individuals2.isEmpty()){
            //add to sr
        }
        List<String> Individuals3 = new ArrayList<String>();
        objectProperty(subject.getObjectSemantics(),PROP_IS_ALONG_Y,Individuals3);
        if(!Individuals3.isEmpty()){
            //add to sr
        }
        List<String> Individuals4 = new ArrayList<String>();
        objectProperty(subject.getObjectSemantics(),PROP_IS_ALONG_Z,Individuals4);
        if(!Individuals4.isEmpty()){
            //add to sr
        }
        List<String> Individuals5 = new ArrayList<String>();
        objectProperty(subject.getObjectSemantics(),PROP_IS_BEHIND_OF,Individuals5);
        if(!Individuals5.isEmpty()){
            //add to sr
        }
        List<String> Individuals6 = new ArrayList<String>();
        objectProperty(subject.getObjectSemantics(),PROP_IS_BELOW_OF,Individuals6);
        if(!Individuals6.isEmpty()){
            //add to sr
        }
        List<String> Individuals7 = new ArrayList<String>();
        objectProperty(subject.getObjectSemantics(),PROP_IS_COAXIAL_WITH,Individuals7);
        if(!Individuals7.isEmpty()){
            //add to sr
        }
        List<String> Individuals8 = new ArrayList<String>();
        objectProperty(subject.getObjectSemantics(),PROP_IS_IN_FRONT_OF,Individuals8);
        if(!Individuals8.isEmpty()){
            //add to sr
        }
        List<String> Individuals9 = new ArrayList<String>();
        objectProperty(subject.getObjectSemantics(),PROP_LEFT,Individuals9);
        if(!Individuals9.isEmpty()){
            //add to sr
        }
        List<String> Individuals10 = new ArrayList<String>();
        objectProperty(subject.getObjectSemantics(),PROP_PARALLEL,Individuals10);
        if(!Individuals10.isEmpty()){
            //add to sr
        }
        List<String> Individuals11 = new ArrayList<String>();
        objectProperty(subject.getObjectSemantics(),PROP_PERPENDICULAR,Individuals11);
        if(!Individuals11.isEmpty()){
            //add to sr
        }
        List<String> Individuals12 = new ArrayList<String>();
        objectProperty(subject.getObjectSemantics(),PROP_RIGHT,Individuals12);
        if(!Individuals12.isEmpty()){
            //add to sr
        }
        //spatialRelationship above=new spatialRelationship();
        //above.setObjectProperty(PROP_IS_ABOVE_OF);
        //above.setObjects(Individuals);
        //sr.add(above);
        return sr;

    }
    public void objectProperty(MORAxioms.ObjectSemantics objProp,String property,List<String> individuals){
        for (MORAxioms.ObjectSemantic obj : objProp) {
            if (obj.toString().contains(property)) {
                MORAxioms.Individuals ind = obj.getValues();
                for (OWLNamedIndividual i : ind) {
                    //add to the string the new score
                    individuals.add(i.toStringID().substring(ONTO_IRI.length() + 1));
                }

            }
        }
    }
    public float ValueOfDataPropertyFloat(MORAxioms.DataSemantics dataProperties, String dataPropertyName){
        //for all the input dataproperties
        for (MORAxioms.DataSemantic i:dataProperties){
            //if the dataproperty coincides with the desired one
            if(i.toString().contains(dataPropertyName)){
                //take only the number
                String str = i.toString().replaceAll("[^\\d.]","");
                //return the number as float
                return Float.parseFloat(str.substring(1));
            }

        }
        return ((float)-1.0);
    }
}

