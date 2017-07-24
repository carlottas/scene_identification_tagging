package it.emarolab.scene_identification_tagging.owloopDescriptor;
import it.emarolab.amor.owlInterface.OWLManipulator;
import it.emarolab.amor.owlInterface.OWLReferences;
import it.emarolab.amor.owlInterface.SemanticRestriction;
import it.emarolab.owloop.aMORDescriptor.MORAxioms;
import it.emarolab.owloop.aMORDescriptor.utility.concept.MORFullConcept;
import it.emarolab.owloop.aMORDescriptor.utility.individual.MORFullIndividual;
import it.emarolab.owloop.core.Concept;
import it.emarolab.scene_identification_tagging.Interfaces.SITBase;
import it.emarolab.scene_identification_tagging.owloopDescriptor.SceneClassDescriptor;
import it.emarolab.scene_identification_tagging.owloopDescriptor.SceneIndividualDescriptor;
import it.emarolab.scene_identification_tagging.owloopDescriptor.SpatialIndividualDescriptor;
import org.apache.jena.ontology.Restriction;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import sit_msgs.*;


import java.util.*;

public class retrievalDescriptor
        implements SITBase{

    private List <sit_msgs.RetrievalSemantic> relations;
    private OWLReferences ontoRef;
    private String name;
    private Set<String> nameRetrieval;
    private Set<String> forgotten;
    private List<String> resetCounter;
    private List<String> userNoForget;
    public retrievalDescriptor(List<sit_msgs.RetrievalSemantic> relations, OWLReferences ontoRef){
        this.relations=relations;
        this.ontoRef=ontoRef;
        this.name= RETRIEVAL.SEMANTIC_RETRIEVAL_NAME;
        retrieval();
    }

    public void retrieval(){
        SceneClassDescriptor learned = new SceneClassDescriptor(name,ontoRef);
        for (sit_msgs.RetrievalSemantic toLearn :relations) {
            learned.addMinObjectRestriction(toLearn.getObjectProperty(), toLearn.getMinCardinality(), toLearn.getPrimitive());
        }
        // update the ontology
        learned.addSuperConcept( CLASS.SCENE);
        learned.writeSemantic(); // cal raesoning
        ontoRef.synchronizeReasoner();
        learned.readSemantic();
        System.out.println(learned);
        String sub=learned.getSubConcept().toString().replaceAll("\\p{P}","");
        Set<OWLClass> equivalentClass= ontoRef.getEquivalentClasses(name);
        Set<String> names= new HashSet<>();
        names.addAll(Arrays.asList(sub.split(" ")));

        //adding equivalent classes
        Set<String> equivalentClasses= new HashSet<>();
        for (OWLClass c:equivalentClass){
            equivalentClasses.add(c.getIRI().toString().toString().substring(ONTO_IRI.length()+1));
        }
        
        names.addAll(equivalentClasses);
        ontoRef.synchronizeReasoner();
        learned.readSemantic();
        learned.delete();
        learned.writeSemantic();
        learned.saveOntology(ONTO_FILE);
        ontoRef.synchronizeReasoner();
        ontoRef.removeClass(name);
        ontoRef.saveOntology(ONTO_FILE);
        Set<String> forgotten= new HashSet<>();
        //check whether they have already been forgotten
        for (String s : names){
            if(s.contains(CLASS.SCENE)) {
                String individualName = FORGETTING.NAME_SEMANTIC_INDIVIDUAL + s.replaceAll("Scene", "");
                MORFullIndividual ind = new MORFullIndividual(individualName, ontoRef);
                ind.readSemantic();
                if (ind.getLiteral(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT).parseBoolean()) {
                    forgotten.add(s);
                }
            }
        }
        this.forgotten=forgotten;
        names.removeAll(forgotten);
        names.remove(name);
        this.nameRetrieval=names;
    }
    public void removeToBeForgot(){
        System.out.println("inside the remove forgot ");
        System.out.println(resetCounter);
        for (String s :resetCounter ){
            MORFullIndividual ind = new MORFullIndividual(FORGETTING.NAME_SEMANTIC_INDIVIDUAL + s.replaceAll("Scene", ""),ontoRef);
            ind.readSemantic();
            ind.removeData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT);
            ind.addData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT,false,true);
            ind.writeSemantic();
            ind.saveOntology(ONTO_FILE);
        }
        for (String s : userNoForget){
            MORFullIndividual ind = new MORFullIndividual(FORGETTING.NAME_SEMANTIC_INDIVIDUAL+s.replaceAll("Scene",""),ontoRef);
            ind.readSemantic();
            ind.removeData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT);
            ind.addData(FORGETTING.NAME_SEMANTIC_DATA_PROPERTY_FORGOT,false,true);
            ind.writeSemantic();
            ind.saveOntology(ONTO_FILE);

        }
    }
    public void updateTimeRetrievalForgotten(){
       this.userNoForget= new ArrayList<>();
       this.resetCounter= new ArrayList<>();
        for (String s:this.forgotten){
            MORFullIndividual ind = new MORFullIndividual(FORGETTING.NAME_SEMANTIC_INDIVIDUAL + s.replaceAll("Scene", ""),ontoRef);
            ind.readSemantic();
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
            ind.saveOntology(ONTO_FILE);
        }

    }
    public Set<String> getNameRetrieval(){return this.nameRetrieval;}
    public List<sit_msgs.RetrievalSemantic> getRelations(){return this.relations;}
    public OWLReferences getOntoRef(){return this.ontoRef;}
    public String getName(){return this.name;}
    public Set<String> getForgotten(){return this.forgotten; }
    public List<String> getResetCounter(){return this.resetCounter;}
    public List<String> getUserNoForget(){return this.userNoForget; }




}

