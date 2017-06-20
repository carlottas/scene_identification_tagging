
package it.emarolab.scene_identification_tagging;


import it.emarolab.owloop.aMORDescriptor.utility.individual.MORFullIndividual;
import it.emarolab.scene_identification_tagging.realObject.Cylinder;
import it.emarolab.scene_identification_tagging.realObject.Sphere;
import it.emarolab.scene_identification_tagging.sceneRepresentation.SpatialRelation;
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

public class SemanticService
        extends AbstractNodeMain
    implements SITBase {
    private static final String SERVICE_NAME = "SemanticService";
    private static final String ONTO_NAME = "ONTO_NAME"; // an arbritary name to refer the ontology
    private static final String NAME = "testScene";
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
                List<sit_msgs.atom> geometricPrimitives = request.getGeometricPrimitives();
                for (sit_msgs.atom g : geometricPrimitives) {
                    float[] coefficient = g.getCoefficients();
                    System.out.println(g.getType());
                    if (SITBase.CLASS.SPHERE.equals(g.getType())) {
                        if (coefficient.length == 4) {
                            Sphere s = new Sphere(ontoRef);
                            s.shouldAddTime(true);
                            s.setCenter(coefficient[0], coefficient[1], coefficient[2]);
                            s.setRadius(coefficient[3]);
                            s.setColor(g.getColor());
                            objects.add(s);
                        } else {
                            System.out.println("Wrong coefficients for sphere!");
                        }

                    } else if (SITBase.CLASS.PLANE.equals(g.getType())) {
                        if (coefficient.length == 7) {
                            Plane p = new Plane(ontoRef);
                            p.shouldAddTime(true);
                            p.setAxis(coefficient[0], coefficient[1], coefficient[2]);
                            p.setCenter(coefficient[3], coefficient[4], coefficient[5]);
                            p.setHessian(coefficient[6]);
                            p.setColor(g.getColor());
                            objects.add(p);
                        } else {
                            System.out.println("Wrong coefficient for Plane ");
                        }

                    } else if (SITBase.CLASS.CYLINDER.equals(g.getType())) {
                        if (coefficient.length == 11) {
                            Cylinder c = new Cylinder(ontoRef);
                            c.shouldAddTime(true);
                            c.setCenter(coefficient[0], coefficient[1], coefficient[2]);
                            c.setAxis(coefficient[3], coefficient[4], coefficient[5]);
                            c.setApex(coefficient[6], coefficient[7], coefficient[8]);
                            c.setRadius(coefficient[9]);
                            c.setHeight(coefficient[10]);
                            c.setColor(g.getColor());
                            objects.add(c);
                        } else {
                            System.out.println("Wrong coefficient for Cylinder");
                        }

                    } else if (SITBase.CLASS.CONE.equals(g.getType())) {
                        if (coefficient.length == 11) {
                            Cone c = new Cone(ontoRef);
                            c.shouldAddTime(true);
                            c.setCenter(coefficient[0], coefficient[1], coefficient[2]);
                            c.setAxis(coefficient[3], coefficient[4], coefficient[5]);
                            c.setApex(coefficient[6], coefficient[7], coefficient[8]);
                            c.setRadius(coefficient[9]);
                            c.setHeight(coefficient[10]);
                            c.setColor(g.getColor());
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
                    recognition1.learn(computeSceneName());
                }

                ontoRef.synchronizeReasoner();
                //take class name
                response.setSceneName(recognition1.getBestRecognitionDescriptor().NameToString(ONTO_NAME.length()+1));
                response.setSubClasses(recognition1.getBestRecognitionDescriptor().SubConceptToString());
                response.setSuperClasses(recognition1.getBestRecognitionDescriptor().SuperConceptToString());
                Atoms atoms = new Atoms();
                for (GeometricPrimitive i : objects) {
                    i.readSemantic();

                    if(i.getTypeIndividual().toString().contains(SITBase.CLASS.SPHERE)){
                        ArrayList<Float> coefficients=new ArrayList<>();
                       //filling the coefficients
                        coefficients.add(i.getLiteral(SITBase.DATA_PROPERTY.RADIUS_SPHERE).parseFloat());
                        Atom g = new Atom (i.getGround().toString().substring(ONTO_NAME.length()+1), CLASS.SPHERE,i.getLiteral(SITBase.COLOR.COLOR_DATA_PROPERTY).getLiteral(),
                                coefficients,computeSR(i));
                        atoms.add(g);

                    }
                    else if (i.getTypeIndividual().toString().contains(SITBase.CLASS.PLANE)){
                        ArrayList<Float> coefficients=new ArrayList<>();
                        coefficients.add(
                                i.getLiteral(DATA_PROPERTY.HESSIAN).parseFloat()
                        );
                        coefficients.add(i.getLiteral(DATA_PROPERTY.AXIS_X).parseFloat());
                        coefficients.add(i.getLiteral(DATA_PROPERTY.AXIS_Y).parseFloat());
                        coefficients.add(i.getLiteral(DATA_PROPERTY.AXIS_Z).parseFloat());
                        Atom g = new Atom(i.getGround().toString().substring(ONTO_NAME.length()+1),CLASS.PLANE,i.getLiteral(SITBase.COLOR.COLOR_DATA_PROPERTY).getLiteral(),
                                coefficients,computeSR(i));
                        atoms.add(g);

                    }
                    else  if (i.getTypeIndividual().toString().contains(CLASS.CYLINDER)){
                        ArrayList<Float> coefficients = new ArrayList<>();
                        coefficients.add(
                                i.getLiteral(DATA_PROPERTY.CYLINDER_HEIGHT).parseFloat());
                        coefficients.add(
                                i.getLiteral(DATA_PROPERTY.CYLINDER_RADIUS).parseFloat());
                        Atom g = new Atom (i.getGround().toString().substring(ONTO_NAME.length()+1), CLASS.CYLINDER,i.getLiteral(SITBase.COLOR.COLOR_DATA_PROPERTY).getLiteral(),
                                coefficients,computeSR(i));
                        atoms.add(g);

                    }
                    else if (i.getTypeIndividual().toString().contains(CLASS.CONE)){
                        ArrayList<Float> coefficients = new ArrayList<>();
                        coefficients.add(
                                i.getLiteral(DATA_PROPERTY.CONE_HEIGHT).parseFloat());
                        coefficients.add(
                                i.getLiteral(DATA_PROPERTY.CONE_RADIUS).parseFloat());
                        Atom g = new Atom (i.getGround().toString().substring(ONTO_NAME.length()+1), CLASS.CONE,i.getLiteral(SITBase.COLOR.COLOR_DATA_PROPERTY).getLiteral(),
                                coefficients,computeSR(i));
                        atoms.add(g);

                    }
                    }
                    atoms.mapInROSMsg(node,response);
                    ontoRef.removeIndividual(recognition1.getSceneDescriptor().getInstance());
                    for (GeometricPrimitive i : objects)
                       ontoRef.removeIndividual(i.getInstance());
                     ontoRef.synchronizeReasoner();
                    recognition1.getBestRecognitionDescriptor().saveOntology(ONTO_FILE);
                }

            };

        }


    public ArrayList<Relation> computeSR(GeometricPrimitive subject){

        ArrayList<Relation> rel = new ArrayList<Relation>();
        subject.readSemantic();

        ArrayList<String> Individuals1 = new ArrayList<String>();
        //Poperty is above of
        objectProperty(subject.getObjectSemantics(),SITBase.SPATIAL_RELATIONS.PROP_IS_ABOVE_OF,Individuals1);
        if(!Individuals1.isEmpty()){
            Relation r= new Relation(Individuals1,SITBase.SPATIAL_RELATIONS.PROP_IS_ABOVE_OF) ;
            rel.add(r);

        }
        //Property is along X
        ArrayList<String> Individuals2 = new ArrayList<String>();
        objectProperty(subject.getObjectSemantics(),SITBase.SPATIAL_RELATIONS.PROP_IS_ALONG_X,Individuals2);
        if(!Individuals2.isEmpty()){
            Relation r= new Relation(Individuals2,SITBase.SPATIAL_RELATIONS.PROP_IS_ABOVE_OF) ;
            rel.add(r);
        }

        //Property is along y
        ArrayList<String> Individuals3 = new ArrayList<String>();
        objectProperty(subject.getObjectSemantics(),SITBase.SPATIAL_RELATIONS.PROP_IS_ALONG_Y,Individuals3);
        if(!Individuals3.isEmpty()){
            Relation r = new Relation(Individuals3,SITBase.SPATIAL_RELATIONS.PROP_IS_ALONG_Y);
            rel.add(r);
        }
        //Property is along z
        ArrayList<String> Individuals4 = new ArrayList<String>();
        objectProperty(subject.getObjectSemantics(),SITBase.SPATIAL_RELATIONS.PROP_IS_ALONG_Z,Individuals4);
        if(!Individuals4.isEmpty()){
            Relation r = new Relation (Individuals4, SITBase.SPATIAL_RELATIONS.PROP_IS_ALONG_Z);
            rel.add(r);
        }
        //Property is behind of
        ArrayList<String> Individuals5 = new ArrayList<String>();
        objectProperty(subject.getObjectSemantics(),SITBase.SPATIAL_RELATIONS.PROP_IS_BEHIND_OF,Individuals5);
        if(!Individuals5.isEmpty()){

            Relation r = new Relation (Individuals5, SITBase.SPATIAL_RELATIONS.PROP_IS_BEHIND_OF);
            rel.add(r);

        }
        //Property is below of
        ArrayList<String> Individuals6 = new ArrayList<String>();
        objectProperty(subject.getObjectSemantics(),SITBase.SPATIAL_RELATIONS.PROP_IS_BELOW_OF,Individuals6);

        if(!Individuals6.isEmpty()){

            Relation r = new Relation (Individuals6,SITBase.SPATIAL_RELATIONS.PROP_IS_BELOW_OF);
            rel.add(r);
        }

        //Property is coaxial with
        ArrayList<String> Individuals7 = new ArrayList<String>();
        objectProperty(subject.getObjectSemantics(),SITBase.SPATIAL_RELATIONS.PROP_IS_COAXIAL_WITH,Individuals7);

        if(!Individuals7.isEmpty()){

            Relation r = new Relation (Individuals7, SITBase.SPATIAL_RELATIONS.PROP_IS_COAXIAL_WITH);
            rel.add(r);
        }
        //Property is in front of

        ArrayList<String> Individuals8 = new ArrayList<String>();
        objectProperty(subject.getObjectSemantics(),SITBase.SPATIAL_RELATIONS.PROP_IS_IN_FRONT_OF,Individuals8);
        if(!Individuals8.isEmpty()){
            Relation r = new Relation (Individuals8, SITBase.SPATIAL_RELATIONS.PROP_IS_IN_FRONT_OF);
            rel.add(r);
        }

        //Property  Left
        ArrayList<String> Individuals9 = new ArrayList<String>();
        objectProperty(subject.getObjectSemantics(),SITBase.SPATIAL_RELATIONS.PROP_LEFT,Individuals9);
        if(!Individuals9.isEmpty()){
            Relation r = new Relation (Individuals9,SITBase.SPATIAL_RELATIONS.PROP_LEFT);
            rel.add(r);
        }

        //Property parallel
        ArrayList<String> Individuals10 = new ArrayList<String>();
        objectProperty(subject.getObjectSemantics(),SITBase.SPATIAL_RELATIONS.PROP_PARALLEL,Individuals10);

        if(!Individuals10.isEmpty()){
            Relation r = new Relation (Individuals10, SITBase.SPATIAL_RELATIONS.PROP_PARALLEL);
            rel.add(r);
        }

        //Property perpendicular
        ArrayList<String> Individuals11 = new ArrayList<String>();
        objectProperty(subject.getObjectSemantics(),SITBase.SPATIAL_RELATIONS.PROP_PERPENDICULAR,Individuals11);

        if(!Individuals11.isEmpty()){

            Relation r = new Relation (Individuals11, SITBase.SPATIAL_RELATIONS.PROP_PERPENDICULAR);
            rel.add(r);

        }

        //Property right
        ArrayList<String> Individuals12 = new ArrayList<String>();
        objectProperty(subject.getObjectSemantics(),SITBase.SPATIAL_RELATIONS.PROP_RIGHT,Individuals12);

        if(!Individuals12.isEmpty()){
            Relation r = new Relation (Individuals12, SITBase.SPATIAL_RELATIONS.PROP_RIGHT);
            rel.add(r);
        }

        return rel ;


    }
    public void objectProperty(MORAxioms.ObjectSemantics objProp, String property, ArrayList<String> individuals){
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
    public float[] ListToArray(ArrayList<Float> floatList){
        float[] floatArray = new float[floatList.size()];
        int i = 0;

        for (Float f : floatList) {
            floatArray[i++] = (f != null ? f : Float.NaN); // Or whatever default you want.
        }
        return floatArray;

    }
    private class Atoms extends HashSet<Atom>{

        /**
         * Map this set into the ROS message returned by this service ({@link sit_test_msgs.SpatialRelation})
         * @param node the bridge to the standard ROS utilities.
         * @param res the service response to be set with the data contained in this set.
         */

    private void mapInROSMsg(ConnectedNode node, SemanticInterfaceResponse res){
            ArrayList<sit_msgs.SpatialAtom> rosAtoms= new ArrayList<sit_msgs.SpatialAtom>();
            for ( Atom s : this){
                sit_msgs.SpatialAtom rosAtom=node.getTopicMessageFactory().newFromType( SpatialAtom._TYPE);
                ArrayList<sit_msgs.SpatialRelationship> rel= new ArrayList<sit_msgs.SpatialRelationship>();
                for ( Relation r :s.getRelations()){
                    //create the relation subject atom
                    sit_msgs.SpatialRelationship rosSR = node.getTopicMessageFactory().newFromType(SpatialRelationship._TYPE);
                    rosSR.setRelation(r.getRelation());
                    rosSR.setObject(r.getObject());
                    rel.add(rosSR);
                }
                rosAtom.setRelations(rel);
                rosAtom.setName(s.getName());
                rosAtom.setColor(s.getColor());
                float[] floatArray = ListToArray(s.getCoefficients());
                rosAtom.setCoefficients(floatArray);
                rosAtom.setType(s.getType());
                rosAtoms.add(rosAtom);

            }
            res.setObjects(rosAtoms);
        }


        @Override
        public boolean add(Atom atom) {
            // simplify the list by not adding redundant relation
            for ( Atom a : this)
                if ( a.equals(atom))
                    return false;
            // add the relation
            return super.add(atom);
        }

        /**
         * @return the textual description of this set.
         */
        @Override
        public String toString() {
            String out = "\n{";
            int cnt = 0;
            for ( Atom s : this) {
                out += "\t" + s.toString();
                if( ++cnt < this.size())
                    out += ";\n";
            }
            return out + "}";
        }
    }
    private class Relation {

        private String relation;
        private ArrayList<String> object ;


        private Relation(ArrayList<String> obj, String rel ) {
            this.object = obj;
            this.relation = rel;
        }
        private Relation (String relation){
            this.relation=relation;
        }

        /**
         * @return the subject of this spatial relation (set on constructor).
         */

        /**
         * @return the object of this spatial relation (set on constructor).
         */
        private ArrayList<String> getObject() {
            return object;
        }

        /**
         * @return the name of this spatial relation (set on constructor).
         */
        private String getRelation() {
            return relation;
        }
        /**
         * @return the name of the inverse relation of this spatial relation (set on constructor).
         */

        /**
         * Set two {@link Relation}s to be equal if those have the same
         * {@code object}s and {@code subjects} as well as {@code relation}.
         * Or, if it is equal to its inverse.
         * @param o the {@link Relation} to test for equality.
         * @return {@code true} if this relation is equal to the given object
         * or if it is equal to the inverse property.
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Relation)) return false;
            Relation that = (Relation) o;

            boolean direct =getObject().equals(that.getObject())
                    & getRelation().equals(that.getRelation());

            return  direct;

        }
        /**
         * It is used to implement {@link #equals(Object)} method.
         * @return a hash code value for this object.
         */
      //  @Override
       // public int hashCode() {
        //    return Objects.hashCode( getObject(), getRelation());
        //}

        /**
         * @return the textual description of this spatial relation.
         */
        @Override
        public String toString() {
            return object+relation.toString();
        }
    }
    private class Atom {

        private String name;
        private String type;
        private ArrayList<Float> coefficients;
        private String color;
        private ArrayList<Relation> relations;


        private Atom(String name, String type) {
            this.name=name;
            this.type=type;
        }
        private Atom(String name, String type,ArrayList<Float> coefficients) {
            this.name=name;
            this.type=type;
            this.coefficients=coefficients;
        }

        private Atom(String name, String type,String color) {
            this.name=name;
            this.type=type;
            this.color=color;
        }

        private Atom(String name, String type,String color,ArrayList<Float> coefficients) {
            this.name=name;
            this.type=type;
            this.color=color;
            this.coefficients=coefficients;
        }

        private Atom(String name, String type,String color,ArrayList<Float> coefficients, ArrayList<Relation> relations) {
            this.name=name;
            this.type=type;
            this.color=color;
            this.coefficients=coefficients;
            this.relations=relations;
        }

        /**
         * @return the type of this object (described as an ontological class).
         */
        private String getName() {
            return name;
        }

        /**
         * @return the name of this object (described as an ontological individual).
         */
        private String getType() {
            return type;
        }

        private String getColor() {
            return color;
        }
        private ArrayList<Float> getCoefficients() {
            return coefficients;
        }
        private ArrayList<Relation> getRelations() {
            return relations;
        }

        /**
         * Set two {@link Atom}s to be equal if those have the same
         * {@code object} names regardless from the types.
         * @param o the {@link Atom} to test for equality.
         * @return {@code true} if this atom is equal to the given object.
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Atom)) return false;
            Atom that = (Atom) o;
            return getName().equals(that.getName()) &&
                    getRelations().equals(that.getRelations()) &&
                    getColor().equals(that.getColor()) &&
                    getType().equals(that.getType()) ;
        }
        /**
         * It is used to implement {@link #equals(Object)} method.
         * @return a hash code value for this object.
         */
        //TODO
        //@Override
        //public int hashCode() {
            //return getObjectId() != null ? getObjectId().hashCode() : 0;
       // }
        /**
         * @return the textual description of this spatial relation.
         */
        //@Override
        //public String toString() {
        //   return objectId + ":'" + shape + "' ";
        //}


    }

    private String computeSceneName(){
        MORFullIndividual counter = new MORFullIndividual(COUNTER.SCENE_COUNTER,
                ONTO_NAME,
                ONTO_FILE,
                ONTO_IRI);
        counter.readSemantic();
        int current_count =counter.getLiteral(COUNTER.VALUE_DATA_PROPERTY).parseInteger();
        counter.removeData(COUNTER.VALUE_DATA_PROPERTY);
        counter.addData(COUNTER.VALUE_DATA_PROPERTY,current_count+1);
        counter.writeSemantic();
        return "Scene"+current_count;

    }
}



