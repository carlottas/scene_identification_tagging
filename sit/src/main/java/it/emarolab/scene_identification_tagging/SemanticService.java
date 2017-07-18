
package it.emarolab.scene_identification_tagging;


import it.emarolab.owloop.aMORDescriptor.MORConcept;
import it.emarolab.owloop.aMORDescriptor.utility.concept.MORFullConcept;
import it.emarolab.owloop.aMORDescriptor.utility.individual.MORFullIndividual;
import it.emarolab.scene_identification_tagging.realObject.Cylinder;
import it.emarolab.scene_identification_tagging.realObject.Sphere;
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
import it.emarolab.scene_identification_tagging.realObject.*;
import it.emarolab.scene_identification_tagging.sceneRepresentation.SceneRepresentation;
import it.emarolab.scene_identification_tagging.owloopDescriptor.retrievalDescriptor;
import it.emarolab.owloop.aMORDescriptor.MORAxioms;

import it.emarolab.amor.owlInterface.SemanticRestriction;
import it.emarolab.owloop.aMORDescriptor.MORAxioms;
import it.emarolab.scene_identification_tagging.owloopDescriptor.SceneClassDescriptor;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.util.*;


public class SemanticService
        extends AbstractNodeMain
    implements SITBase {
    private static final String SERVICE_NAME = "SemanticService";
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
                int decision = request.getDecision();
                //MEMORIZATION
                if (decision == 1) {
                    // initialise objects
                    Set<GeometricPrimitive> objects = fromPITtoSIT(request.getGeometricPrimitives(), ontoRef);
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
                        recognition1.learn(computeSceneName());
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
                    Atoms atoms = fromSemanticToEpisodic(objects);
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
                        response.setRetrievaled(ListRetrieval);


                    }

                }
                //forgetting
                else if (decision==3){

                }
                //recognition
                else if (decision==4){
                    // initialise objects
                    Set<GeometricPrimitive> objects = fromPITtoSIT(request.getGeometricPrimitives(), ontoRef);
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
                        System.out.println("Recognised with best confidence: " + recognition1.getRecognitionConfidence() );
                        System.out.println("Best recognised class: " + recognition1.getBestRecognitionDescriptor());
                        System.out.println("Other recognised classes: " + recognition1.getSceneDescriptor().getTypeIndividual());
                        response.setSceneName(recognition1.getBestRecognitionDescriptor().NameToString(ONTO_NAME.length() + 1));
                        Atoms atoms = fromSemanticToEpisodic(objects);
                        atoms.mapInROSMsg(node, response);
                        ontoRef.removeIndividual(recognition1.getSceneDescriptor().getInstance());
                        for (GeometricPrimitive i : objects)
                            ontoRef.removeIndividual(i.getInstance());
                        ontoRef.synchronizeReasoner();
                    }
                }
                if(!request.getToBeForget().isEmpty()){
                    for(String s : request.getToBeForget())
                    {
                        String individualName = FORGETTING.NAME_SEMANTIC_INDIVIDUAL + s.replaceAll("Scene", "");
                        MORFullIndividual ind = new MORFullIndividual(individualName, ontoRef);
                        ind.readSemantic();
                        ind.removeData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT);
                        ind.addData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT,true,true);
                        ind.writeSemantic();
                        ind.saveOntology(ONTO_FILE);
                    }

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
    public Set<GeometricPrimitive> fromPITtoSIT(List<sit_msgs.atom> geometricPrimitives,OWLReferences ontoRef){
        Set<GeometricPrimitive> objects = new HashSet<>();
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
                    p.setCenter(coefficient[0], coefficient[1], coefficient[2]);
                    p.setAxis(coefficient[3], coefficient[4], coefficient[5]);
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
                    c.setApex(coefficient[3], coefficient[4], coefficient[5]);
                    c.setAxis(coefficient[6], coefficient[7], coefficient[8]);
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
                    c.setApex(coefficient[3], coefficient[4], coefficient[5]);
                    c.setAxis(coefficient[6], coefficient[7], coefficient[8]);
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
        return objects;
    }
    public List<String> computeFirstSuperClass(SceneRepresentation recognition1, OWLReferences ontoRef ){
        List<String> firstSupClass= new ArrayList<>();
        for (OWLClass s : recognition1.getBestRecognitionDescriptor().getSuperConcept()){
            MORAxioms.Concepts SupCl= recognition1.getBestRecognitionDescriptor().getSuperConcept();
            System.out.println("super classes of the semantic item before the removal\n"+SupCl);
            //todo sometimes problems of concurrent modificiation to be checked
            SupCl.remove(s);
            System.out.println("super classes of the semantic item after the removal\n"+SupCl);
            MORFullConcept ind= new MORFullConcept(s.toStringID().substring(ONTO_IRI.length() + 1),ontoRef);
            ind.readSemantic();
            MORAxioms.Concepts supCl= ind.getSuperConcept();
            System.out.println("super classes of the current super class\n"+supCl);

            if(SupCl.equals(supCl)){
                firstSupClass.add(s.toStringID().substring(ONTO_IRI.length()+1));
            }
        }
        return firstSupClass;
    }
    public List<String> computeIsFirstSuperClassOf(List<String> subClasses,OWLReferences ontoRef,SceneRepresentation recognition1){
        List<String> isFirstSupClassOf= new ArrayList<>();
        MORAxioms.Concepts sameLevelClasses= new MORAxioms.Concepts();
        //finding the classes which are at the same hierarcy grade of the semantic item
        for (String s:subClasses){
            MORFullConcept ind= new MORFullConcept(s,ontoRef);
            ind.readSemantic();
            MORAxioms.Concepts cl= ind.getSuperConcept();
            for (OWLClass l:cl){
                MORFullConcept ind2 = new MORFullConcept(l.toStringID().substring(ONTO_IRI.length() + 1),ontoRef);
                ind2.readSemantic();
                MORAxioms.Concepts cl2= ind2.getSuperConcept();
                if(recognition1.getBestRecognitionDescriptor().getSuperConcept().equals(cl2)){
                    sameLevelClasses.add(l);
                }
            }
            System.out.println("superClasses without the removal \n"+cl);
            for(OWLClass l: sameLevelClasses) {
                cl.remove(l);
            }

            if(cl.equals(recognition1.getBestRecognitionDescriptor().getSuperConcept())){
                isFirstSupClassOf.add(s);
            }

        }
        System.out.println("classes at the same level\n"+sameLevelClasses);
        return isFirstSupClassOf;
    }
    public Atoms fromSemanticToEpisodic(Set<GeometricPrimitive> objects){
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
        return atoms ;


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

                    for(String o:r.getObject()) {
                        sit_msgs.SpatialRelationship rosSR = node.getTopicMessageFactory().newFromType(SpatialRelationship._TYPE);
                        rosSR.setRelation(r.getRelation());
                        rosSR.setObject(r.getObject());
                        rel.add(rosSR);
                    }
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
    public class Relation {

        private String relation;
        private ArrayList<String> object ;


       public Relation(ArrayList<String> obj, String rel ) {
            this.object = obj;
            this.relation = rel;
        }
        public Relation (String relation){
            this.relation=relation;
        }

        /**
         * @return the subject of this spatial relation (set on constructor).
         */

        /**
         * @return the object of this spatial relation (set on constructor).
         */
        public ArrayList<String> getObject() {
            return object;
        }

        /**
         * @return the name of this spatial relation (set on constructor).
         */
        public String getRelation() {
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



