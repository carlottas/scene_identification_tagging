package it.emarolab.scene_identification_tagging;

import it.emarolab.amor.owlInterface.OWLReferences;
import it.emarolab.amor.owlInterface.OWLReferencesInterface;
import it.emarolab.owloop.aMORDescriptor.MORAxioms;
import it.emarolab.owloop.aMORDescriptor.utility.individual.MORFullIndividual;
import it.emarolab.owloop.core.ObjectProperty;
import it.emarolab.scene_identification_tagging.realObject.*;
import it.emarolab.scene_identification_tagging.realObject.Cylinder;
import it.emarolab.scene_identification_tagging.realObject.Sphere;
import it.emarolab.scene_identification_tagging.sceneRepresentation.EpisodicScene;
import it.emarolab.scene_identification_tagging.sceneRepresentation.SpatialRelation;
import it.emarolab.scene_identification_tagging.sceneRepresentation.Relation;
import javafx.scene.shape.*;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
//import jdk.internal.org.objectweb.asm.tree.analysis.Value;
import sit_msgs.*;
import org.ros.node.ConnectedNode;
import org.ros.node.service.ServiceResponseBuilder;
import org.ros.internal.message.Message;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.Node;
import org.ros.node.parameter.ParameterTree;
import java.awt.image.AreaAveragingScaleFilter;
import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;



public class EpisodicService
        extends AbstractNodeMain
        implements SITBase {

    private static final String SERVICE_NAME = "EpisodicService";
    private static final String ONTO_NAME = "ONTO_NAME"; // an arbritary name to refer the ontology
    private static final String NAME = "testScene";

    public boolean initParam(ConnectedNode node) {

        // stat the service
        node.newServiceServer(
                getServerName(), // set service name
                EpisodicInterface._TYPE, // set ROS service message
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

    public ServiceResponseBuilder<EpisodicInterfaceRequest, EpisodicInterfaceResponse> getService(ConnectedNode node) {
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
                OWLReferences ontoRef = OWLReferencesInterface.OWLReferencesContainer.newOWLReferenceFromFileWithPellet(
                        EPISODIC_ONTO_NAME, EPISODIC_ONTO_FILE, EPISODIC_ONTO_IRI, true);
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
                    ArrayList<EpisodicPrimitive> Primitives = fromSemanticToEpisodic(object, ontoRef);
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
                        episodicScene.Learn(ontoRef, ComputeName(CLASS.SCENE));
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

                }
                //forgetting
                else if (decision==3){

                }
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
    public ArrayList<EpisodicPrimitive>  fromSemanticToEpisodic(Atoms object,OWLReferences ontoRef){
        ArrayList<EpisodicPrimitive> Primitives = new ArrayList<>();
        for (Atom a : object){
            if(a.getType().equals(CLASS.SPHERE)){
                EpisodicSphere s= new EpisodicSphere(ComputeName(CLASS.SPHERE),ontoRef);
                s.setColor(a.getColor());
                s.setRadius(a.getCoefficients().get(0));
                s.setRelations(a.getRelations());
                s.setName(a.getName());
                s.shouldAddTime(true);
                Primitives.add(s);
            }
            else if (a.getType().equals(CLASS.PLANE)){
                EpisodicPlane p = new EpisodicPlane(ComputeName(CLASS.PLANE),ontoRef);
                p.setColor(a.getColor());
                p.setHessian(a.coefficients.get(0));
                p.setName(a.getName());
                p.setRelations(a.getRelations());
                p.shouldAddTime(true);
                Primitives.add(p);

            }
            else if (a.getType().equals(CLASS.CYLINDER)){
                EpisodicCylinder c = new EpisodicCylinder(ComputeName(CLASS.CYLINDER),ontoRef);
                c.setColor(a.getColor());
                c.setHeight(a.getCoefficients().get(0));
                c.setRadius(a.getCoefficients().get(1));
                c.setName(a.getName());
                c.setRelations(a.getRelations());
                c.shouldAddTime(true);
                Primitives.add(c);
            }
            else if (a.getType().equals(CLASS.CONE)){
                EpisodicCone c = new EpisodicCone(ComputeName(CLASS.CONE),ontoRef);
                c.setColor(a.getColor());
                c.setHeight(a.getCoefficients().get(0));
                c.setRadius(a.getCoefficients().get(1));
                c.setName(a.getName());
                c.setRelations(a.getRelations());
                c.shouldAddTime(true);
                Primitives.add(c);
            }
        }
        //update the name with the new name computed
        for (EpisodicPrimitive i : Primitives){
            ArrayList<Relation> newRelation = new ArrayList<>();
            for(Relation r : i.getRelations()){
                ArrayList<String> newObjects= new ArrayList<>();
                for (EpisodicPrimitive j:Primitives) {
                    for (String s : r.getObject()) {
                        if (s.equals(j.getName())) {
                            newObjects.add(j.getGround().toString().substring(EPISODIC_ONTO_NAME.length() + 1));
                        }

                    }
                }
                newRelation.add(new Relation( newObjects,r.getRelation()));
            }
            i.setRelations(newRelation);
        }
        return  Primitives;


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
    public class Atoms extends HashSet<Atom>{

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
        public void MapFromRosMsg(List<sit_msgs.SpatialAtom> rosAtoms){

            for(sit_msgs.SpatialAtom a: rosAtoms){
                ArrayList<Relation> rel= new ArrayList<>();
                for (sit_msgs.SpatialRelationship rosSR:a.getRelations()) {
                    ArrayList<String> objects= new ArrayList<>();
                    for(int i=0; i<rosSR.getObject().size();i++){
                        objects.add(rosSR.getObject().get(i));
                    }
                    Relation r = new Relation(objects,rosSR.getRelation());
                    rel.add(r);
                }
                ArrayList<Float> coefficients=new ArrayList<>();
                for ( int i = 0; i<a.getCoefficients().length;i++){
                    coefficients.add(a.getCoefficients()[i]);

                }
                this.add(new Atom(a.getName(),a.getType(),a.getColor(),coefficients,rel));
            }
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
    public class Atom {

        private String name;
        private String type;
        private ArrayList<Float> coefficients;
        private String color;
        private ArrayList<Relation> relations;


        public Atom(String name, String type) {
            this.name=name;
            this.type=type;
        }
        public Atom(String name, String type,ArrayList<Float> coefficients) {
            this.name=name;
            this.type=type;
            this.coefficients=coefficients;
        }

        public Atom(String name, String type,String color) {
            this.name=name;
            this.type=type;
            this.color=color;
        }

        public Atom(String name, String type,String color,ArrayList<Float> coefficients) {
            this.name=name;
            this.type=type;
            this.color=color;
            this.coefficients=coefficients;
        }

        public Atom(String name, String type,String color,ArrayList<Float> coefficients, ArrayList<Relation> relations) {
            this.name=name;
            this.type=type;
            this.color=color;
            this.coefficients=coefficients;
            this.relations=relations;
        }

        /**
         * @return the type of this object (described as an ontological class).
         */
        public String getName() {
            return name;
        }

        /**
         * @return the name of this object (described as an ontological individual).
         */
        public String getType() {
            return type;
        }

        public String getColor() {
            return color;
        }
        public ArrayList<Float> getCoefficients() {
            return coefficients;
        }
        public ArrayList<Relation> getRelations() {
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
    public String ComputeName (String Type){
        String Counter= new String ();
        String Prefix= new String ();
        if (Type.equals(CLASS.SPHERE)){
            Counter = COUNTER.SPHERE_COUNTER;
            Prefix = INDIVIDUAL.PREFIX_SPHERE;
        }
        else if (Type.equals(CLASS.PLANE)){
            Counter = COUNTER.PLANE_COUNTER;
            Prefix = INDIVIDUAL.PREFIX_PLANE;
        }
        else if (Type.equals(CLASS.CONE)){
            Counter = COUNTER.CONE_COUNTER;
            Prefix = INDIVIDUAL.PREFIX_CONE;
        }
        else if (Type.equals(CLASS.CYLINDER)){
            Counter = COUNTER.CYLINDER_COUNTER;
            Prefix = INDIVIDUAL.PREFIX_CYLINDER;

        }
        else if (Type.contains(CLASS.SCENE)){
            Counter=COUNTER.EPISODIC_SCENE_COUNTER;
            Prefix=INDIVIDUAL.EPISODIC_SCENE;
        }
        else {
            return null;
        }
        MORFullIndividual counter = new MORFullIndividual(Counter,
                EPISODIC_ONTO_NAME,
                EPISODIC_ONTO_FILE,
                EPISODIC_ONTO_IRI);
        counter.readSemantic();
        int current_count =counter.getLiteral(COUNTER.VALUE_DATA_PROPERTY).parseInteger();
        counter.removeData(COUNTER.VALUE_DATA_PROPERTY);
        counter.addData(COUNTER.VALUE_DATA_PROPERTY,current_count+1);
        counter.writeSemantic();

        return Prefix +current_count;
    }


}
