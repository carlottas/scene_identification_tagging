package it.emarolab.scene_identification_tagging.sceneRepresentation;


import it.emarolab.amor.owlInterface.OWLReferences;
import it.emarolab.amor.owlInterface.ObjectPropertyRelations;
import it.emarolab.owloop.aMORDescriptor.MORAxioms;
import it.emarolab.owloop.aMORDescriptor.utility.MORConceptBase;
import it.emarolab.owloop.aMORDescriptor.utility.concept.MORFullConcept;
import it.emarolab.owloop.aMORDescriptor.utility.individual.MORFullIndividual;
import it.emarolab.owloop.core.Concept;
import it.emarolab.scene_identification_tagging.Interfaces.SITBase;
import it.emarolab.scene_identification_tagging.owloopDescriptor.SceneClassDescriptor;
import it.emarolab.scene_identification_tagging.owloopDescriptor.SceneIndividualDescriptor;
import it.emarolab.scene_identification_tagging.owloopDescriptor.SpatialIndividualDescriptor;
import it.emarolab.scene_identification_tagging.realObject.EpisodicPrimitive;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import java.util.*;

public class EpisodicScene implements SITBase{


    private Set< SpatialRelation> relations;
    private Set<colorRelation> colorRelations;
    private String EpisodicSceneName;
    private String SemanticSceneName;
    private String SupportName ;
    private List<String> SubClasses;
    private List <String> SuperClasses;
    private long time = System.currentTimeMillis();
    private boolean addTime = false;
    private boolean newSupport;

    /**
     * This constructor assume that the given {@code object} have all object property that
     * describes spatial relations between two objects (already populated from SWRL rules).
     * It initialises all the fields of {@code this} class.
     * @param objects the objects describing spatial relations.
     * @param ontoRef the ontology that the SIT should manipulate.
     */
    public EpisodicScene(Collection< ? extends SpatialIndividualDescriptor> objects, OWLReferences ontoRef,String SemanticSceneName){
        relations = computeSceneRelations(objects);
        this.SemanticSceneName=SemanticSceneName;
        colorRelations= computeColorRelation(objects);
    }
    private Set< SpatialRelation> computeSceneRelations(Collection<? extends SpatialIndividualDescriptor> objects) {
        Set< SpatialRelation> relations = new HashSet<>();
        if ( ! objects.isEmpty())
            // hp: object properties of individual belonging to "GeometricPrimitive" are all spatial relations
            for ( SpatialIndividualDescriptor o : objects) {
                for ( MORAxioms.ObjectSemantic s : o.getObjectSemantics()) {
                    for (OWLNamedIndividual  i : s.getValues()) {
                        relations.add(new SpatialRelation(o, s.getSemantic(), i));
                    }
                }
            }
        return relations;
    }

    private Set< colorRelation> computeColorRelation(Collection<? extends SpatialIndividualDescriptor> objects) {
        Set< colorRelation> colorRelations = new HashSet<>();
        if ( ! objects.isEmpty())
            for ( SpatialIndividualDescriptor o : objects) {
                String Color= o.getLiteral(COLOR.COLOR_DATA_PROPERTY).getLiteral();
                colorRelations.add(new colorRelation(o,Color));
            }

        return colorRelations;
    }

    // get spatial relation between a Scene and an Object from the spatial relation between two Objects
    private OWLObjectProperty getSpatialRelation(OWLReferences ontology, OWLObjectProperty relation) {
        String relationName = relation.getIRI().getRemainder().get();
        String sceneRelationName = OBJECT_PROPERTY.SCENE_SPATIAL_PRFIX + relationName;
        return ontology.getOWLObjectProperty( sceneRelationName);
    }
    // get spatial relation between a Scene and an Object from the spatial relation between two Objects
    private OWLObjectProperty getColorRelation(OWLReferences ontology, String color) {
        String sceneRelationName = OBJECT_PROPERTY.SCENE_SPATIAL_PRFIX + color;
        return ontology.getOWLObjectProperty( sceneRelationName);
    }
    //return true if the class has been initialed, false if it already exists
    public boolean InitializeClasses(OWLReferences ontoRef){
        if(this.SemanticSceneName.equals(CLASS.SCENE)){
            return false;
        }
        MORFullConcept SuperClass= new MORFullConcept(CLASS.SCENE,ontoRef);
        SuperClass.readSemantic();
        if(SuperClass.getSubConcept().toString().contains(SemanticSceneName)){
            return false;
        }
        else {
            //TODO non ha senso usare mor full concept, devi definirne uno piu piccolo
            MORFullConcept currentClass= new MORFullConcept(SemanticSceneName,ontoRef);
            currentClass.addSuperConcept(CLASS.SCENE);
            currentClass.writeSemantic();
            Set<OWLClass> disj= new HashSet<OWLClass>();
            for(OWLClass c:SuperClass.getSubConcept()){
                if(!c.getIRI().toString().contains("Nothing")) {
                    System.out.println(c.getIRI().toString());
                    System.out.println("qui non dovrei esserci");
                    disj.add(c);
                }
            }
            ontoRef.makeDisjointClasses(disj);
            return true;
        }

    }
    public boolean ShouldLearn(OWLReferences ontoRef){

        if(newSupport){return true;}
        ArrayList<EpisodicLearn> description = new ArrayList<>();
        ArrayList<EpisodicLearn> ColorDescription = new ArrayList<>();
        for (SpatialRelation r:relations){
            EpisodicLearn episodicLearn= new EpisodicLearn(getSpatialRelation(ontoRef, r.getRelation()), r);
            if(!description.contains(episodicLearn)){
                description.add(episodicLearn);
            }
        }
        for(colorRelation c: colorRelations){
            ColorDescription.add(new EpisodicLearn(getColorRelation(ontoRef,c.getColor()),c));
        }
        //Only for the spatial relation check
        MORFullConcept classes= new MORFullConcept(SemanticSceneName,ontoRef);
        ontoRef.synchronizeReasoner();
        classes.readSemantic();

        //for all the individuals which belongs to the related class
        for(OWLNamedIndividual i : classes.getIndividualClassified()){
            int count =0 ;
            MORFullIndividual ind= new MORFullIndividual(i,ontoRef);
            ontoRef.synchronizeReasoner();
            ind.readSemantic();
            if((description.size()+ColorDescription.size())==countNumberOfRelations(ind)) {
                System.out.println("inside the if sizes are equal ");
                count = countNumberOfEqualObjectProperty(description, ind, ontoRef);
                count += countNumberOfEqualObjectProperty(ColorDescription, ind, ontoRef);
                System.out.println(count+"\n");
                if (count == description.size() + ColorDescription.size()) {
                    for(MORAxioms.ObjectSemantic obj: ind.getObjectSemantics()){
                        if(obj.toString().contains(SUPPORT.HAS_SCENE_SUPPORT)) {
                            if (obj.getValues().toString().contains(SupportName)){
                                System.out.println("same support");
                                this.EpisodicSceneName = ind.getGround().toString().substring(EPISODIC_ONTO_NAME.length() + 1);
                                ind.removeData(DATA_PROPERTY.TIME);
                                ind.addData(DATA_PROPERTY.TIME,time,true);
                                ind.writeSemantic();
                                return false;
                            }
                        }
                    }

                }
            }
        }

        return true;

    }
    public void Learn(OWLReferences ontoRef,String Name){
        this.EpisodicSceneName=Name;
        SceneIndividualDescriptor scene= new SceneIndividualDescriptor(this.EpisodicSceneName, ontoRef);
        scene.addTypeIndividual(SemanticSceneName);
        for( SpatialRelation r : relations){
            EpisodicLearn description =new EpisodicLearn(getSpatialRelation(ontoRef,r.getRelation()),r);

            scene.addObject(description.getRelation(),description.getIndividual());
        }
        for (colorRelation c: colorRelations){
            EpisodicLearn description = new EpisodicLearn(getColorRelation(ontoRef,c.getColor()),c);
            scene.addObject(description.getRelation(),description.getIndividual());
        }
        if (addTime)
            scene.addData( DATA_PROPERTY.TIME, time, true);
        scene.addData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT,false,true);
        scene.addData(FORGETTING.NAME_DATA_PROPERTY_RETRIEVAL_FORGOT,(float)0.0);
        scene.addObject(SUPPORT.HAS_SCENE_SUPPORT,SupportName);
        scene.writeSemantic();
        scene.saveOntology(EPISODIC_ONTO_FILE);

    }
    public int countNumberOfRelations(MORFullIndividual ind){
        //Hyp all the object properties are related to spatial relations and color
        //plus one for the support (hence the minus 1)
        int count=0;
        for(MORAxioms.ObjectSemantic i : ind.getObjectSemantics()){
            count+=i.getValues().size();
        }

        return count-1;



    }
    public int countNumberOfEqualObjectProperty(ArrayList<EpisodicLearn> relation,MORFullIndividual ind,OWLReferences ontoRef){
       int count = 0 ;
        ontoRef.synchronizeReasoner();
        ind.readSemantic();
        for (EpisodicLearn j : relation) {
            //For all the object property of the current scene Item
           for (MORAxioms.ObjectSemantic obj : ind.getObjectSemantics()){

                   //if they are the same object property
                   if (obj.getSemantic().equals(j.getRelation())) {
                       for (OWLNamedIndividual value : obj.getValues()) {
                           //if it hold for an individual that belong to the same classes
                           //hence it has the same geometric features
                           ontoRef.synchronizeReasoner();
                           MORFullIndividual ind1 = new MORFullIndividual(value, ontoRef);
                           ind1.readSemantic();
                           MORFullIndividual ind2 = new MORFullIndividual(j.getIndividual(), ontoRef);
                           ind2.readSemantic();
                           if (ind1.getTypeIndividual().equals(ind2.getTypeIndividual())) {

                               count++;
                           }
                       }
                   }
               }

           }
        return count ;
    }
    /**
     * The spatial relation object properties between oll the individuals
     * given on the constructor.
     * @return the spatial relations between objects
     */
    public Set<SpatialRelation> getRelations() {
        return relations;
    }

    /**
     * Return if this class add (or not) a time stamp data property to
     * the concrete scene individual (i.e.: {@link DATA_PROPERTY#TIME})
     * @return {@code true} if the time stamp is introduced in the ontology. {@code false} otherwise
     */
    public boolean isAddingTime() {
        return addTime;
    }

    /**
     * Enable/disable the adding of a time stamp data property to
     * the concrete scene individual (i.e.: {@link DATA_PROPERTY#TIME})
     * @param addTime {@code true} enable time stamping, {@code false} disable it.
     */
    public void setAddingTime(boolean addTime) {
        this.addTime = addTime;
    }
    public void setSupportName(String SupportName){this.SupportName=SupportName;}
    public String getSupportName(){return this.SupportName;}
    public void setEpisodicSceneName(String EpisodicSceneName){this.EpisodicSceneName=EpisodicSceneName;}
    public void setSemanticSceneName(String SemanticSceneName){this.SemanticSceneName=SemanticSceneName;}
    public void setSubClasses(List<String> SubClasses){this.SubClasses=SubClasses;}
    public void setSuperClasses(List<String> superClasses){this.SuperClasses=superClasses;}
    public String getEpisodicSceneName(){return this.EpisodicSceneName;}
    public String getSemanticSceneName(){return this.SemanticSceneName;}
    public List<String> getSubClasses(){return this.SubClasses;}
    public List<String> getSuperClasses(){return this.SuperClasses;}
    public void InitializeSupport(OWLReferences ontoRef){
        MORFullConcept suppClass= new MORFullConcept(SUPPORT.SUPPORT_CLASS_NAME,ontoRef);
        suppClass.readSemantic();
        for(OWLNamedIndividual i : suppClass.getIndividualClassified()) {
            MORFullIndividual ind = new MORFullIndividual(i,ontoRef);
            ind.readSemantic();
            if(ind.getGround().toString().substring(EPISODIC_ONTO_NAME.length()+1).equals(SupportName)){
                newSupport=false;
                return;
            }
        }
        MORFullIndividual supp=new MORFullIndividual(SupportName,ontoRef);
        supp.addTypeIndividual(SUPPORT.SUPPORT_CLASS_NAME);
        supp.writeSemantic();
        newSupport=true;
    }


    private class EpisodicLearn{

        private OWLObjectProperty relation;
        private OWLNamedIndividual individual;

        public EpisodicLearn(OWLObjectProperty relation , SpatialRelation r){
            this.relation=relation;
            this.individual = r.getSubject().getIndividual();
        }
        public EpisodicLearn(OWLObjectProperty sceneRelation, colorRelation colorRelation) {
            this.relation = sceneRelation;
            this.individual = colorRelation.getSubject().getIndividual();

        }

        private OWLObjectProperty getRelation() {
            return relation;
        }
        private OWLNamedIndividual getIndividual() {
            return individual; // used in equal
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EpisodicLearn)) return false;

            EpisodicLearn that = (EpisodicLearn) o;

            if (getRelation() != null ? !getRelation().equals(that.getRelation()) : that.getRelation() != null)
                return false;
            return getIndividual() != null ? getIndividual().equals(that.getIndividual()) : that.getIndividual() == null;
        }

        @Override
        public int hashCode() {
            int result = getRelation() != null ? getRelation().hashCode() : 0;
            result = 31 * result + (getIndividual() != null ? getIndividual().hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return relation.getIRI().getRemainder().get() + " min " + individual;
        }

}

}
