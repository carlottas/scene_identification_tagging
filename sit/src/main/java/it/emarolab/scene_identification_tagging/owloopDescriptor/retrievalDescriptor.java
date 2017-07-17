package it.emarolab.scene_identification_tagging.owloopDescriptor;
import it.emarolab.amor.owlInterface.OWLReferences;
import it.emarolab.owloop.aMORDescriptor.MORAxioms;
import it.emarolab.owloop.aMORDescriptor.utility.concept.MORFullConcept;
import it.emarolab.owloop.aMORDescriptor.utility.individual.MORFullIndividual;
import it.emarolab.scene_identification_tagging.SITBase;
import it.emarolab.scene_identification_tagging.owloopDescriptor.SceneClassDescriptor;
import it.emarolab.scene_identification_tagging.owloopDescriptor.SceneIndividualDescriptor;
import it.emarolab.scene_identification_tagging.owloopDescriptor.SpatialIndividualDescriptor;
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
        learned.writeSemanticInconsistencySafe( true); // cal raesoning
        ontoRef.synchronizeReasoner();
        learned.readSemantic();
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
        Set<String> forgotten= new HashSet<>();

        ontoRef.removeClass(name);
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
        names.removeAll(forgotten);
        names.remove(name);
        this.nameRetrieval=names;
    }

    public Set<String> getNameRetrieval(){return this.nameRetrieval;}
    public List<sit_msgs.RetrievalSemantic> getRelations(){return this.relations;}
    public OWLReferences getOntoRef(){return this.ontoRef;}
    public String getName(){return this.name;}




}

