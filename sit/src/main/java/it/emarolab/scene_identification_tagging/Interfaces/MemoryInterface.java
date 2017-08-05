package it.emarolab.scene_identification_tagging.Interfaces;

import it.emarolab.amor.owlInterface.OWLReferences;
import it.emarolab.owloop.aMORDescriptor.MORAxioms;
import it.emarolab.owloop.aMORDescriptor.utility.concept.MORFullConcept;
import it.emarolab.owloop.aMORDescriptor.utility.individual.MORFullIndividual;
import it.emarolab.scene_identification_tagging.owloopDescriptor.SceneClassDescriptor;
import it.emarolab.scene_identification_tagging.realObject.*;
import it.emarolab.scene_identification_tagging.sceneRepresentation.SceneRepresentation;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import sit_msgs.retrievalAtom;
import it.emarolab.scene_identification_tagging.sceneRepresentation.Atom;
import it.emarolab.scene_identification_tagging.sceneRepresentation.Atoms;
import it.emarolab.scene_identification_tagging.sceneRepresentation.Relation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import it.emarolab.scene_identification_tagging.Interfaces.SITBase;

public interface MemoryInterface
{

    public  class memory implements  SITBase {
        public  memory(){}
        public static void objectPropertyValues(MORAxioms.ObjectSemantics objProp, String property, List<String> individuals,String IRI) {
        for (MORAxioms.ObjectSemantic obj : objProp) {
            if (obj.toString().contains(property)) {
                MORAxioms.Individuals ind = obj.getValues();
                for (OWLNamedIndividual i : ind) {
                    //add to the string the new score
                    //TODO change such that it depends on the onto ref and not on the string SCORE IRI ONTO
                    individuals.add(i.toStringID().substring(IRI.length() + 1));
                }

            }
        }
    }
        public static void objectProperty(MORAxioms.ObjectSemantics objProp, String property, ArrayList<String> individuals,String IRI){
            for (MORAxioms.ObjectSemantic obj : objProp) {
                if (obj.toString().contains(property)) {
                    MORAxioms.Individuals ind = obj.getValues();
                    for (OWLNamedIndividual i : ind) {
                        //add to the string the new score
                        individuals.add(i.toStringID().substring(IRI.length() + 1));
                    }

                }
            }
        }
        public static void changeUserNoForget(String Name, OWLReferences ontoRef, boolean state){
            MORFullIndividual individual = new MORFullIndividual(Name,ontoRef);
            individual.readSemantic();
            individual.removeData(SCORE.SCORE_PROP_USER_NO_FORGET);
            if(state){
                individual.addData(SCORE.SCORE_PROP_USER_NO_FORGET,true, true);
            }
            else{
                individual.addData(SCORE.SCORE_PROP_USER_NO_FORGET,false,true);
            }
            individual.writeSemantic();
            individual.saveOntology(SCORE.SCORE_FILE_PATH);
        }
        public static void resetCounter (String name, OWLReferences ontoRef){
            MORFullIndividual ind = new MORFullIndividual(name,ontoRef);
            ind.readSemantic();
            ind.removeData(SCORE.SCORE_PROP_TIMES_LOW_SCORE);
            ind.addData(SCORE.SCORE_PROP_TIMES_LOW_SCORE,0);
            ind.removeData(SCORE.SCORE_PROP_TIMES_TO_BE_FORGOTTEN);
            ind.addData(SCORE.SCORE_PROP_TIMES_TO_BE_FORGOTTEN,0);
            ind.removeData(SCORE.SCORE_PROP_TIMES_TO_BE_FORGOTTEN);
            ind.addData(SCORE.SCORE_PROP_TIMES_TO_BE_FORGOTTEN,0);
            ind.writeSemantic();
            ind.saveOntology(SCORE.SCORE_FILE_PATH);
        }
        public static String computeSceneName(OWLReferences ontoRef){
            MORFullIndividual counter = new MORFullIndividual(COUNTER.SCENE_COUNTER,
                    ontoRef);
            counter.readSemantic();
            int current_count =counter.getLiteral(COUNTER.VALUE_DATA_PROPERTY).parseInteger();
            counter.removeData(COUNTER.VALUE_DATA_PROPERTY);
            counter.addData(COUNTER.VALUE_DATA_PROPERTY,current_count+1);
            counter.writeSemantic();
            return "Scene"+current_count;

        }
        public static float[] ListToArray(ArrayList<Float> floatList){
            float[] floatArray = new float[floatList.size()];
            int i = 0;

            for (Float f : floatList) {
                floatArray[i++] = (f != null ? f : Float.NaN); // Or whatever default you want.
            }

            return floatArray;

        }
        public static Set<GeometricPrimitive> fromPITtoSIT(List<sit_msgs.atom> geometricPrimitives, OWLReferences ontoRef){
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
        public static List<String> computeFirstSuperClass(SceneRepresentation recognition1, OWLReferences ontoRef ){
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
        public static List<String> computeIsFirstSuperClassOf(List<String> subClasses,OWLReferences ontoRef,SceneRepresentation recognition1){
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
        public static List<String> computePossibleSupportScene(String supportName,OWLReferences ontoRef){
            List<String> possibleSupportScenes=new ArrayList<>();
            MORFullConcept supportClass = new MORFullConcept(SUPPORT.SUPPORT_CLASS_NAME,ontoRef);
            supportClass.readSemantic();
            for(OWLNamedIndividual i: supportClass.getIndividualClassified()){
                if (i.getIRI().toString().substring(EPISODIC_ONTO_IRI.length() + 1).equals(supportName)){
                    MORFullIndividual ind = new MORFullIndividual(i,ontoRef);
                    ind.readSemantic();
                    objectPropertyValues(ind.getObjectSemantics(),SUPPORT.OBJECT_PROPERTY_IS_SUPPORT_OF,possibleSupportScenes,EPISODIC_ONTO_IRI);
                }
            }
            return possibleSupportScenes;

        }
        public static List<String> computePossibleSpatialRelationshipScene(List<sit_msgs.objectPropertyRetrieval> spatialRelationshipRetrievals,
                                                                    OWLReferences ontoRef){

            List<List<String>> ListPossibleScenesSpatialRelationship = new ArrayList<>();
            List<String> posssibleSceneSpatialRelationship = new ArrayList<>();
            for (sit_msgs.objectPropertyRetrieval obj : spatialRelationshipRetrievals) {
                List<String> possibleScene = new ArrayList<>();
                sit_msgs.retrievalAtom subject = obj.getSubject();
                sit_msgs.retrievalAtom object = obj.getObject();
                String relation = obj.getRelationship();
                //TODO change, create a concept for this
                //not doing it without the subject because it would be semantic retrieval
                MORFullConcept classSubject = new MORFullConcept(subject.getLabel(), ontoRef);
                classSubject.readSemantic();
                MORAxioms.Individuals individuals = classSubject.getIndividualClassified();
                List<EpisodicPrimitive> equalsToSubject = new ArrayList<>();
                //checking the individuals which are equal to the request
                for (OWLNamedIndividual i : individuals) {
                    EpisodicPrimitive ind = new EpisodicPrimitive(i, ontoRef);
                    ind.readSemantic();
                    MORAxioms.Concepts cl = ind.getTypeIndividual();
                    if (ind.getLiteral(COLOR.COLOR_DATA_PROPERTY).getLiteral().equals(subject.getColor())) {
                        if (checkClasses(cl.toString(), subject)) {
                            equalsToSubject.add(ind);
                        }
                    }
                }
                for (EpisodicPrimitive i : equalsToSubject) {
                    //check whether the same spatial relationship hold
                    i.readSemantic();
                    System.out.println("SUBJECT");
                    System.out.println(i.getGround());
                    if (i.getObjectSemantics().toString().contains(obj.getRelationship())) {
                        OWLNamedIndividual currentObj = i.getObject(obj.getRelationship());
                        MORFullIndividual IndivudalObject = new MORFullIndividual(currentObj, ontoRef);
                        IndivudalObject.readSemantic();
                        System.out.println("OBJECT");
                        System.out.println(IndivudalObject.getGround());
                        if (IndivudalObject.getLiteral(COLOR.COLOR_DATA_PROPERTY).getLiteral().equals(object.getColor())) {
                            if (checkClasses(IndivudalObject.getTypeIndividual().toString(), object)) {
                                //possible Scene
                                IndivudalObject.readSemantic();
                                List<String> scenes= new ArrayList<>();
                                objectPropertyValues(IndivudalObject.getObjectSemantics(),DATA_PROPERTY.BELONG_TO_SCENE,scenes,EPISODIC_ONTO_IRI);
                                possibleScene.addAll(scenes);
                            }
                        }
                    }
                }
                ListPossibleScenesSpatialRelationship.add(possibleScene);
            }

            posssibleSceneSpatialRelationship=computeCommonElementListsString(ListPossibleScenesSpatialRelationship);

            return posssibleSceneSpatialRelationship;


        }


        public static String ComputeName (String Type,OWLReferences ontoRef){
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
            ontoRef.synchronizeReasoner();
            MORFullIndividual counter = new MORFullIndividual(Counter,
                    ontoRef);
            counter.readSemantic();
            int current_count =counter.getLiteral(COUNTER.VALUE_DATA_PROPERTY).parseInteger();
            counter.removeData(COUNTER.VALUE_DATA_PROPERTY);
            counter.addData(COUNTER.VALUE_DATA_PROPERTY,current_count+1);
            counter.writeSemantic();
            counter.saveOntology(EPISODIC_ONTO_FILE);

            return Prefix +current_count;
        }
        public static List<String> computeCommonElementListsString(List<List<String>> listOfList){
            List<String> intersection= new ArrayList<>();
            for (List<String> i :listOfList) {
                if (!i.isEmpty()) {
                    intersection=i;
                    break;
                }
            }
            for (List<String> l : listOfList) {
                if (!l.isEmpty()) {
                    intersection.retainAll(l);
                }
            }
            return intersection;

        }
        public static List<String> computePossibleTimeIntervalScenes(String timeInterval, OWLReferences ontoRef){
            List<String> possibleTimeIntervalScenes= new ArrayList<>();
            MORFullConcept TimeClass  = new MORFullConcept(timeInterval,ontoRef);
            TimeClass.readSemantic();
            MORAxioms.Individuals inds= TimeClass.getIndividualClassified();
            for (OWLNamedIndividual i : inds) {
                possibleTimeIntervalScenes.add(i.getIRI().toString().substring(EPISODIC_ONTO_IRI.length()+1));
            }
            return possibleTimeIntervalScenes;

        }
        public static String timeIntervalClass(int time){
            String timeClass="";
            switch(time)
            {
                case 1 : {
                    timeClass = TIME.TEN_MINUTES_CLASS;
                    break;
                }
                case 2 : {
                    timeClass=TIME.THIRTHY_MINUTES_CLASS;
                    break;
                }
                case 3 : {
                    timeClass=TIME.ONE_HOUR_CLASS;
                    break ;
                }
                default:{
                    timeClass=TIME.NO_TIME;
                }

            }
            return timeClass;


        }
        public static boolean checkClasses(String classes,sit_msgs.retrievalAtom atom){
            List<String> classAtom = new ArrayList<>();
            String prefix1="";
            if (atom.getLabel().equals(CLASS.SPHERE)){
                prefix1=CLASS.SPHERE;
            }
            else if (atom.getLabel().equals(CLASS.CONE)){
                prefix1=CLASS.CONE;
            }
            else if (atom.getLabel().equals(CLASS.CYLINDER)){
                prefix1=CLASS.CYLINDER;
            }
            else if (atom.getLabel().equals(CLASS.PLANE)){
                prefix1=CLASS.PLANE;
            }

            for(sit_msgs.geometricCharacteristic g : atom.getGeometricFeatures()){
                String prefix2=g.getCharacteristic();
                if(g.getInterval()==1){
                    classAtom.add(prefix1+prefix2+GEOMETRICFEATURES.SMALLER_THAN_FIVE);
                }
                else if (g.getInterval()==2){
                    classAtom.add(prefix1+prefix2+GEOMETRICFEATURES.INCLUDED_IN_TEN_FIVE);
                }
                else if (g.getInterval()==3){
                    classAtom.add(prefix1+prefix2+GEOMETRICFEATURES.BIGGER_THAN_TEN);
                }
            }
            int count = 0;
            for (String s : classAtom){
                if(classes.contains(s)){
                    count++;
                }
            }
            return count==classAtom.size();
        }
        public static List<String> computePossiblePrimitiveScenes(List<retrievalAtom> retrievalPrimitives,
                                                           OWLReferences ontoRef){
            List<List<String>> ListPossiblePrimitiveScenes= new ArrayList<>();
            for (retrievalAtom atom: retrievalPrimitives){
                MORFullConcept classAtom = new MORFullConcept(atom.getLabel(), ontoRef);
                classAtom.readSemantic();
                MORAxioms.Individuals individuals = classAtom.getIndividualClassified();
                List<String> equalsAtom = new ArrayList<>();
                //checking the individuals which are equal to the request
                for (OWLNamedIndividual i : individuals) {
                    MORFullIndividual ind = new MORFullIndividual(i, ontoRef);
                    ind.readSemantic();
                    MORAxioms.Concepts cl = ind.getTypeIndividual();
                    if (ind.getLiteral(COLOR.COLOR_DATA_PROPERTY).getLiteral().equals(atom.getColor())) {
                        if (memory.checkClasses(cl.toString(),atom)) {
                            List<String> scenes= new ArrayList<>();
                            memory.objectPropertyValues(ind.getObjectSemantics(),DATA_PROPERTY.BELONG_TO_SCENE,scenes,EPISODIC_ONTO_IRI);
                            equalsAtom.addAll(scenes);
                        }
                    }
                }
                ListPossiblePrimitiveScenes.add(equalsAtom);

            }

            return memory.computeCommonElementListsString(ListPossiblePrimitiveScenes);

        }
        public static ArrayList<Relation> computeSR(GeometricPrimitive subject){

            ArrayList<Relation> rel = new ArrayList<Relation>();
            subject.readSemantic();

            ArrayList<String> Individuals1 = new ArrayList<String>();
            //Poperty is above of
            memory.objectProperty(subject.getObjectSemantics(),SITBase.SPATIAL_RELATIONS.PROP_IS_ABOVE_OF,Individuals1,EPISODIC_ONTO_IRI);
            if(!Individuals1.isEmpty()){
                Relation r= new Relation(Individuals1,SITBase.SPATIAL_RELATIONS.PROP_IS_ABOVE_OF) ;
                rel.add(r);

            }
            //Property is along X
            ArrayList<String> Individuals2 = new ArrayList<String>();
            memory.objectProperty(subject.getObjectSemantics(),SITBase.SPATIAL_RELATIONS.PROP_IS_ALONG_X,Individuals2,EPISODIC_ONTO_IRI);
            if(!Individuals2.isEmpty()){
                Relation r= new Relation(Individuals2,SITBase.SPATIAL_RELATIONS.PROP_IS_ABOVE_OF) ;
                rel.add(r);
            }

            //Property is along y
            ArrayList<String> Individuals3 = new ArrayList<String>();
            memory.objectProperty(subject.getObjectSemantics(),SITBase.SPATIAL_RELATIONS.PROP_IS_ALONG_Y,Individuals3,EPISODIC_ONTO_IRI);
            if(!Individuals3.isEmpty()){
                Relation r = new Relation(Individuals3,SITBase.SPATIAL_RELATIONS.PROP_IS_ALONG_Y);
                rel.add(r);
            }
            //Property is along z
            ArrayList<String> Individuals4 = new ArrayList<String>();
            memory. objectProperty(subject.getObjectSemantics(),SITBase.SPATIAL_RELATIONS.PROP_IS_ALONG_Z,Individuals4,EPISODIC_ONTO_IRI);
            if(!Individuals4.isEmpty()){
                Relation r = new Relation (Individuals4, SITBase.SPATIAL_RELATIONS.PROP_IS_ALONG_Z);
                rel.add(r);
            }
            //Property is behind of
            ArrayList<String> Individuals5 = new ArrayList<String>();
            memory.objectProperty(subject.getObjectSemantics(),SITBase.SPATIAL_RELATIONS.PROP_IS_BEHIND_OF,Individuals5,EPISODIC_ONTO_IRI);
            if(!Individuals5.isEmpty()){

                Relation r = new Relation (Individuals5, SITBase.SPATIAL_RELATIONS.PROP_IS_BEHIND_OF);
                rel.add(r);

            }
            //Property is below of
            ArrayList<String> Individuals6 = new ArrayList<String>();
            memory.objectProperty(subject.getObjectSemantics(),SITBase.SPATIAL_RELATIONS.PROP_IS_BELOW_OF,Individuals6,EPISODIC_ONTO_IRI);

            if(!Individuals6.isEmpty()){

                Relation r = new Relation (Individuals6,SITBase.SPATIAL_RELATIONS.PROP_IS_BELOW_OF);
                rel.add(r);
            }

            //Property is coaxial with
            ArrayList<String> Individuals7 = new ArrayList<String>();
            memory.objectProperty(subject.getObjectSemantics(),SITBase.SPATIAL_RELATIONS.PROP_IS_COAXIAL_WITH,Individuals7,EPISODIC_ONTO_IRI);

            if(!Individuals7.isEmpty()){

                Relation r = new Relation (Individuals7, SITBase.SPATIAL_RELATIONS.PROP_IS_COAXIAL_WITH);
                rel.add(r);
            }
            //Property is in front of

            ArrayList<String> Individuals8 = new ArrayList<String>();
            memory.objectProperty(subject.getObjectSemantics(),SITBase.SPATIAL_RELATIONS.PROP_IS_IN_FRONT_OF,Individuals8,EPISODIC_ONTO_IRI);
            if(!Individuals8.isEmpty()){
                Relation r = new Relation (Individuals8, SITBase.SPATIAL_RELATIONS.PROP_IS_IN_FRONT_OF);
                rel.add(r);
            }

            //Property  Left
            ArrayList<String> Individuals9 = new ArrayList<String>();
            memory.objectProperty(subject.getObjectSemantics(),SITBase.SPATIAL_RELATIONS.PROP_LEFT,Individuals9,EPISODIC_ONTO_IRI);
            if(!Individuals9.isEmpty()){
                Relation r = new Relation (Individuals9,SITBase.SPATIAL_RELATIONS.PROP_LEFT);
                rel.add(r);
            }

            //Property parallel
            ArrayList<String> Individuals10 = new ArrayList<String>();
            memory.objectProperty(subject.getObjectSemantics(),SITBase.SPATIAL_RELATIONS.PROP_PARALLEL,Individuals10,EPISODIC_ONTO_IRI);

            if(!Individuals10.isEmpty()){
                Relation r = new Relation (Individuals10, SITBase.SPATIAL_RELATIONS.PROP_PARALLEL);
                rel.add(r);
            }

            //Property perpendicular
            ArrayList<String> Individuals11 = new ArrayList<String>();
            memory.objectProperty(subject.getObjectSemantics(),SITBase.SPATIAL_RELATIONS.PROP_PERPENDICULAR,Individuals11,EPISODIC_ONTO_IRI);

            if(!Individuals11.isEmpty()){

                Relation r = new Relation (Individuals11, SITBase.SPATIAL_RELATIONS.PROP_PERPENDICULAR);
                rel.add(r);

            }

            //Property right
            ArrayList<String> Individuals12 = new ArrayList<String>();
            memory.objectProperty(subject.getObjectSemantics(),SITBase.SPATIAL_RELATIONS.PROP_RIGHT,Individuals12,EPISODIC_ONTO_IRI);

            if(!Individuals12.isEmpty()){
                Relation r = new Relation (Individuals12, SITBase.SPATIAL_RELATIONS.PROP_RIGHT);
                rel.add(r);
            }

            return rel ;


        }
        public static ArrayList<EpisodicPrimitive>  fromSemanticToEpisodic(Atoms object,OWLReferences ontoRef){
            ArrayList<EpisodicPrimitive> Primitives = new ArrayList<>();
            for (Atom a : object){
                if(a.getType().equals(CLASS.SPHERE)){
                    System.out.println("inside the class sphere");
                    EpisodicSphere s= new EpisodicSphere(memory.ComputeName(CLASS.SPHERE,ontoRef),ontoRef);
                    s.setColor(a.getColor());
                    s.setRadius(a.getCoefficients().get(0));
                    s.setRelations(a.getRelations());
                    s.setName(a.getName());
                    s.shouldAddTime(true);
                    Primitives.add(s);
                }
                else if (a.getType().equals(CLASS.PLANE)){
                    EpisodicPlane p = new EpisodicPlane(memory.ComputeName(CLASS.PLANE,ontoRef),ontoRef);
                    p.setColor(a.getColor());
                    p.setHessian(a.getCoefficients().get(0));
                    p.setName(a.getName());
                    p.setRelations(a.getRelations());
                    p.shouldAddTime(true);
                    Primitives.add(p);

                }
                else if (a.getType().equals(CLASS.CYLINDER)){
                    EpisodicCylinder c = new EpisodicCylinder(memory.ComputeName(CLASS.CYLINDER,ontoRef),ontoRef);
                    c.setColor(a.getColor());
                    c.setHeight(a.getCoefficients().get(0));
                    c.setRadius(a.getCoefficients().get(1));
                    c.setName(a.getName());
                    c.setRelations(a.getRelations());
                    c.shouldAddTime(true);
                    Primitives.add(c);
                }
                else if (a.getType().equals(CLASS.CONE)){
                    EpisodicCone c = new EpisodicCone(memory.ComputeName(CLASS.CONE,ontoRef),ontoRef);
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

        public static Atoms fromSemanticToEpisodic(Set<GeometricPrimitive> objects,String OntoName){
            Atoms atoms = new Atoms();
            for (GeometricPrimitive i : objects) {
                i.readSemantic();

                if(i.getTypeIndividual().toString().contains(SITBase.CLASS.SPHERE)){
                    ArrayList<Float> coefficients=new ArrayList<>();
                    //filling the coefficients
                    coefficients.add(i.getLiteral(SITBase.DATA_PROPERTY.RADIUS_SPHERE).parseFloat());
                    Atom g = new Atom (i.getGround().toString().substring(OntoName.length()+1), CLASS.SPHERE,i.getLiteral(SITBase.COLOR.COLOR_DATA_PROPERTY).getLiteral(),
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
                    Atom g = new Atom(i.getGround().toString().substring(OntoName.length()+1),CLASS.PLANE,i.getLiteral(SITBase.COLOR.COLOR_DATA_PROPERTY).getLiteral(),
                            coefficients,computeSR(i));
                    atoms.add(g);

                }
                else  if (i.getTypeIndividual().toString().contains(CLASS.CYLINDER)){
                    ArrayList<Float> coefficients = new ArrayList<>();
                    coefficients.add(
                            i.getLiteral(DATA_PROPERTY.CYLINDER_HEIGHT).parseFloat());
                    coefficients.add(
                            i.getLiteral(DATA_PROPERTY.CYLINDER_RADIUS).parseFloat());
                    Atom g = new Atom (i.getGround().toString().substring(OntoName.length()+1), CLASS.CYLINDER,i.getLiteral(SITBase.COLOR.COLOR_DATA_PROPERTY).getLiteral(),
                            coefficients,computeSR(i));
                    atoms.add(g);

                }
                else if (i.getTypeIndividual().toString().contains(CLASS.CONE)){
                    ArrayList<Float> coefficients = new ArrayList<>();
                    coefficients.add(
                            i.getLiteral(DATA_PROPERTY.CONE_HEIGHT).parseFloat());
                    coefficients.add(
                            i.getLiteral(DATA_PROPERTY.CONE_RADIUS).parseFloat());
                    Atom g = new Atom (i.getGround().toString().substring(OntoName.length()+1), CLASS.CONE,i.getLiteral(SITBase.COLOR.COLOR_DATA_PROPERTY).getLiteral(),
                            coefficients,computeSR(i));
                    atoms.add(g);

                }
            }
            return atoms ;


        }
        public static List<String> RetrievalSemanticEpisodic(List<String> classes, OWLReferences ontoRef,Set<String> forgotten ){
            List<String > individuals = new ArrayList<>();
            System.out.println("IMPORTANT.INSIDE RETRIEVAL EPISODIC FROM SEMANTIC");
            System.out.println("ontology"+ontoRef);
            for (String s : classes) {
                if (!s.equals("owlNothing")) {
                    //defining the ontological class
                    SceneClassDescriptor currentClass = new SceneClassDescriptor(s, ontoRef);
                    //read the ontology
                    currentClass.readSemantic();
                    //getting the individuals classified
                    MORAxioms.Individuals i = currentClass.getIndividualClassified();
                    for (OWLNamedIndividual ind:i){
                        MORFullIndividual individual= new MORFullIndividual(ind,ontoRef);
                        individual.readSemantic();
                        //if the episodic item has not been already forgot add to the list
                        if(individual.getLiteral(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT).parseBoolean()) {
                            //todo Change with ground
                            forgotten.add(ind.getIRI().toString().substring(EPISODIC_ONTO_IRI.length() + 1));
                        }
                        individuals.add(ind.getIRI().toString().substring(EPISODIC_ONTO_IRI.length()+1));
                    }
                }
            }
            return  individuals;
        }

        public static float updateCounterRetrievalForgetting(List<String> resetCounter,List<String> userNoForget,String Name,OWLReferences ontoRef){
            MORFullIndividual ind = new MORFullIndividual(Name,ontoRef);
            ind.readSemantic();
            float counter = ind.getLiteral(FORGETTING.NAME_DATA_PROPERTY_RETRIEVAL_FORGOT).parseFloat();
            float newCounter = 0;
            if (counter<FORGETTING.FIRST_THRESHOLD){
                newCounter=counter+FORGETTING.INCREMENT_ONE;
                if (newCounter>=FORGETTING.FIRST_THRESHOLD){
                    resetCounter.add(Name);

                }

            }
            else if (counter<FORGETTING.SECOND_THRESHOLD && counter>=FORGETTING.FIRST_THRESHOLD){
                newCounter=counter+FORGETTING.INCREMENT_TWO;
                if(newCounter>=FORGETTING.SECOND_THRESHOLD){
                    resetCounter.add(Name);
                }

            }
            else if (counter<FORGETTING.THIRD_THRESHOLD &&counter>=FORGETTING.SECOND_THRESHOLD){
                newCounter=counter+FORGETTING.INCREMENT_THREE;
                if(newCounter>=3){
                    userNoForget.add(Name);
                }
            }
            ind.removeData(FORGETTING.NAME_DATA_PROPERTY_RETRIEVAL_FORGOT);
            ind.addData(FORGETTING.NAME_DATA_PROPERTY_RETRIEVAL_FORGOT,newCounter);
            ind.writeSemantic();
            ind.saveOntology(EPISODIC_ONTO_FILE);
            return newCounter;

        }
        public static void removeUserNoForgetEpisodic(String Name, OWLReferences ontoRef,String ONTO_FILE){
            MORFullIndividual ind = new MORFullIndividual(Name,ontoRef);
            ind.readSemantic();
            ind.removeData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT);
            ind.addData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT,false,true);
            ind.writeSemantic();
            ind.saveOntology(ONTO_FILE);
        }
        public static void addPrimitivesEpisodic( ArrayList<EpisodicPrimitive> Primitives){
            for (EpisodicPrimitive i : Primitives) {
                for (EpisodicPrimitive j : Primitives)
                    if (!i.equals(j))
                        j.addDisjointIndividual(i.getInstance());
                i.getObjectSemantics().clear(); // clean previus spatial relation
                i.writeSemantic();
                i.saveOntology(EPISODIC_ONTO_FILE);

            }

        }
        public static void deleteEpisodicItem(String s , OWLReferences ontoRef){
            //declare the ontoogical individual
            MORFullIndividual delete= new MORFullIndividual(s,ontoRef);
            //read the ontology
            delete.readSemantic();
            //check the primitives that must be deleted
            List<String> primitivesDelete= new ArrayList<>();
            memory.objectPropertyValues(delete.getObjectSemantics(),OBJECT_PROPERTY.HAS_SCENE_PRIMITIVE,primitivesDelete,EPISODIC_ONTO_IRI);
            //remove the Episodic Item
            ontoRef.removeIndividual(s);
            //remove the primitives
            for(String i:primitivesDelete){
                ontoRef.removeIndividual(i);
            }
            //save the ontology
            ontoRef.saveOntology(EPISODIC_ONTO_FILE);
            }
        public static void updateTimeEpisodic(MORFullIndividual ind){
            ind.readSemantic();
            ind.removeData(DATA_PROPERTY.TIME);
            ind.addData(DATA_PROPERTY.TIME,System.currentTimeMillis());
            ind.writeSemantic();
            ind.saveOntology(EPISODIC_ONTO_FILE);

        }
        public static void updateTimeEpisodic(String s, OWLReferences ontoRef){
            MORFullIndividual ind= new MORFullIndividual(s,ontoRef);
            updateTimeEpisodic(ind);
        }
    }

}

