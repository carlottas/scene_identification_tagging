package it.emarolab.scene_identification_tracking.semanticSceneLibrary.aMOR.semantic;

import it.emarolab.amor.owlInterface.OWLReferences;
import it.emarolab.scene_identification_tracking.semanticSceneLibrary.core.Semantic;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.util.Collection;
import java.util.Set;

/**
 * Created by bubx on 17/03/17.
 */
public interface MORSemantic extends Semantic{

    @SuppressWarnings("Duplicates")
    class MORType
            implements Semantic.Type<OWLReferences,OWLNamedIndividual,MORAxiom.MORTyped> {

        private MORAxiom.MORTyped types = new MORAxiom.MORTyped();

        public MORType(){}
        public MORType( Set<OWLClass> types){
            this.types.getParents().addAll( types);
        }

        @Override
        public void set( MORAxiom.MORTyped types) {
            this.types = types;
        }

        @Override
        public MORAxiom.MORTyped get() {
            return types;
        }


        @Override
        public MORAxiom.MORTyped query(OWLReferences ontology, OWLNamedIndividual instance) {
            return new MORAxiom.MORTyped( ontology.getIndividualClasses(instance));
        }

        @Override
        public <Y> void add(OWLReferences ontology, OWLNamedIndividual instance, Y type) {
            if ( type instanceof Set){
                for ( Object t : (Set) type){
                    if ( t instanceof OWLClass)
                        add( ontology, instance, (OWLClass) t);
                }
            } else if ( type instanceof OWLClass)
                add( ontology, instance, (OWLClass) type);
            //todo log error
        }
        private void add( OWLReferences ontology, OWLNamedIndividual instance, OWLClass type){
            ontology.addIndividualB2Class( instance, type);
        }

        @Override
        public <Y> void remove(OWLReferences ontology, OWLNamedIndividual instance, Y type) {
            if ( type instanceof Set){
                for ( Object t : (Set) type){
                    if ( t instanceof OWLClass)
                        remove( ontology, instance, (OWLClass) t);
                }
            } else if ( type instanceof OWLClass)
                remove( ontology, instance, (OWLClass) type);
            //todo log error
        }
        private void remove(OWLReferences ontology, OWLNamedIndividual instance, OWLClass type) {
            ontology.removeIndividualB2Class( instance, type);
        }

        @Override
        public String toString() {
            return "MORType{" +
                    "types=" + types +
                    '}';
        }
    }

    class MORHierarchy
            implements Semantic.Hierarchy<OWLReferences,OWLClass,MORAxiom.MORHierarchised>{

        private MORAxiom.MORHierarchised node = new MORAxiom.MORHierarchised();

        public MORHierarchy(){}
        public MORHierarchy( Set<OWLClass> parents, Set<OWLClass> children){
            this.node.getParents().addAll( parents);
            this.node.getChildren().addAll( children);
        }
        public MORHierarchy( OWLClass parent, OWLClass child){
            if (parent != null)
                this.node.getParents().add( parent);
            if (child != null)
                this.node.getChildren().add( child);
        }
        public MORHierarchy(MORAxiom.MORHierarchised axiom) {
            this.get().getParents().addAll( axiom.getParents());
            this.get().getChildren().addAll( axiom.getChildren());
        }

        @Override
        public void set(MORAxiom.MORHierarchised node) {
            this.node = node;
        }

        @Override
        public MORAxiom.MORHierarchised get() {
            return node;
        }

        @Override
        public MORAxiom.MORHierarchised query(OWLReferences ontology, OWLClass instance) {
            Set<OWLClass> children = ontology.getSubClassOf(instance);
            Set<OWLClass> parent = ontology.getSuperClassOf(instance);
            return new MORAxiom.MORHierarchised( parent, children);
        }

        @Override
        public <Y> void addParents(OWLReferences ontology, OWLClass instance, Y type) {
            if ( type instanceof Set){
                for ( Object t : (Set) type){
                    if ( t instanceof OWLClass)
                        add( ontology, (OWLClass) t, instance);
                }
            } else if ( type instanceof OWLClass)
                add( ontology, (OWLClass) type, instance);
            //todo log error
        }
        @Override
        public <Y> void addChildren(OWLReferences ontology, OWLClass instance, Y type) {
            if ( type instanceof Set){
                for ( Object t : (Set) type){
                    if ( t instanceof OWLClass)
                        add( ontology, instance, (OWLClass) t);
                }
            } else if ( type instanceof OWLClass)
                add( ontology, instance, (OWLClass) type);
            //todo log error
        }
        private void add( OWLReferences ontology, OWLClass parent, OWLClass child){
            ontology.addSubClassOf( parent, child);
        }

        @Override
        public <Y> void removeParents(OWLReferences ontology, OWLClass instance, Y type) {
            if ( type instanceof Set){
                for ( Object t : (Set) type){
                    if ( t instanceof OWLClass)
                        remove( ontology, (OWLClass) t, instance);
                }
            } else if ( type instanceof OWLClass)
                remove( ontology, (OWLClass) type, instance);
            //todo log error
        }
        @Override
        public <Y> void removeChildren(OWLReferences ontology, OWLClass instance, Y type) {
            if ( type instanceof Set){
                for ( Object t : (Set) type){
                    if ( t instanceof OWLClass)
                        remove( ontology, instance, (OWLClass) t);
                }
            } else if ( type instanceof OWLClass)
                remove( ontology, instance, (OWLClass) type);
            //todo log error
        }
        private void remove( OWLReferences ontology, OWLClass parent, OWLClass child){
            ontology.removeSubClassOf( parent, child);
        }
    }

    class MORLiteral
            implements Connection<OWLReferences,OWLNamedIndividual,OWLDataProperty,MORAxiom.MORLiterised> {

        private OWLDataProperty property;
        private MORAxiom.MORLiterised literal = new MORAxiom.MORLiterised();

        public MORLiteral(){
        }
        public MORLiteral(OWLDataProperty property){
            this.setSemantic( property);
        }
        public MORLiteral(OWLDataProperty property, OWLLiteral value){
            this.setSemantic( property);
            this.literal.setAtom( value);
        }

        @Override
        public void set(MORAxiom.MORLiterised link) {
            this.literal = link;
        }

        @Override
        public MORAxiom.MORLiterised get() {
            return literal;
        }


        @Override
        public MORAxiom.MORLiterised query(OWLReferences ontology, OWLNamedIndividual instance) {
            //ontology.setOWLEnquirerIncludesInferences( false);
            OWLLiteral value = ontology.getOnlyDataPropertyB2Individual( instance, getSemantic());
            //ontology.setOWLEnquirerIncludesInferences( true);
            return new MORAxiom.MORLiterised( value);
        }

        @Override
        public OWLDataProperty getSemantic() {
            return property;
        }

        @Override
        public void setSemantic(OWLDataProperty property) {
            this.property = property;
        }

        @Override
        public <V> void add(OWLReferences ontology, OWLNamedIndividual instance, OWLDataProperty property, V value) {
            if( value instanceof OWLLiteral)
                ontology.addDataPropertyB2Individual( instance, property, (OWLLiteral) value);
            // else // todo log
        }

        @Override
        public <V> void remove(OWLReferences ontology, OWLNamedIndividual instance, OWLDataProperty property, V value) {
            if( value instanceof OWLLiteral)
                ontology.removeDataPropertyB2Individual( instance, property, (OWLLiteral) value);
            // else // todo log
        }
    }

    class MORLiterals
            implements Connections<OWLReferences,OWLNamedIndividual,OWLDataProperty,MORAxiom.MORMultiLiterised>{

        private OWLDataProperty property;
        private MORAxiom.MORMultiLiterised literals = new MORAxiom.MORMultiLiterised();

        public MORLiterals(){
        }
        public MORLiterals(OWLDataProperty property){
            this.setSemantic( property);
        }
        public MORLiterals(OWLDataProperty property, OWLLiteral literal){
            this.setSemantic( property);
            this.literals.add( new MORAxiom.MORLiterised( literal));
        }
        public MORLiterals(OWLDataProperty property, Collection< OWLLiteral> literals){
            this.setSemantic( property);
            for (OWLLiteral l : literals)
                this.literals.add( new MORAxiom.MORLiterised( l));
        }
        public MORLiterals(OWLDataProperty property, MORAxiom.MORLiterised literal){
            this.setSemantic( property);
            this.literals.add( literal);
        }
        public MORLiterals(OWLDataProperty property, MORAxiom.MORMultiLiterised literals){
            this.setSemantic( property);
            this.literals.addAll( literals);
        }

        @Override
        public void set(MORAxiom.MORMultiLiterised literals) {
            this.literals = literals;
        }

        @Override
        public MORAxiom.MORMultiLiterised get() {
            return literals;
        }

        @Override
        public MORAxiom.MORMultiLiterised query(OWLReferences ontology, OWLNamedIndividual instance) {
            //ontology.setOWLEnquirerIncludesInferences( false);
            Set< OWLLiteral> value = ontology.getDataPropertyB2Individual( instance, getSemantic());
            //ontology.setOWLEnquirerIncludesInferences( true);
            return new MORAxiom.MORMultiLiterised( value);
        }

        @Override
        public OWLDataProperty getSemantic() {
            return property;
        }

        @Override
        public void setSemantic(OWLDataProperty property) {
            this.property = property;
        }

        @Override
        public <Y> void add(OWLReferences ontology, OWLNamedIndividual instance, OWLDataProperty property, Y literal) {
            if ( literal instanceof Set){
                for ( Object l : (Set) literal){
                    if ( l instanceof OWLLiteral)
                        add( ontology, instance, (OWLLiteral) l);
                }
            } else if ( literal instanceof OWLLiteral)
                add( ontology, instance, (OWLLiteral) literal);
            //todo log error
        }
        private void add( OWLReferences ontology, OWLNamedIndividual instance, OWLLiteral literal){
            ontology.addDataPropertyB2Individual( instance, getSemantic(), literal);
        }

        @Override
        public <Y> void remove(OWLReferences ontology, OWLNamedIndividual instance, OWLDataProperty property, Y literal) {
            if ( literal instanceof Set){
                for ( Object l : (Set) literal){
                    if ( l instanceof OWLLiteral)
                        remove( ontology, instance, (OWLLiteral) l);
                }
            } else if ( literal instanceof OWLLiteral)
                remove( ontology, instance, (OWLLiteral) literal);
            //todo log error
        }
        private void remove(OWLReferences ontology, OWLNamedIndividual instance, OWLLiteral literal) {
            ontology.removeDataPropertyB2Individual( instance, getSemantic(), literal);
        }

    }


/*
    class MORMinCardinalityRestriction
            implements Semantic.ClassRestriction<OWLReferences,OWLClass,MORAxiom.MORMultiMinCardinalised> {

        MORAxiom.MORMultiMinCardinalised cardinalities = new MORAxiom.MORMultiMinCardinalised();

        @Override
        public void set(MORAxiom.MORMultiMinCardinalised atom) {
            this.cardinalities = atom;
        }

        @Override
        public MORAxiom.MORMultiMinCardinalised get() {
            return cardinalities;
        }


        @Override
        public MORAxiom.MORMultiMinCardinalised query(OWLReferences ontology, OWLClass instance) {
            MORAxiom.MORMultiMinCardinalised out = new MORAxiom.MORMultiMinCardinalised();
            Set<OWLEnquirer.ClassRestriction> restrictions = ontology.getClassRestrictions(instance);
            for ( OWLEnquirer.ClassRestriction r : restrictions)
                if (r.isMinRestriction())
                    out.add( r.getObjectProperty(), r.getObjectRestriction(), r.getCardinality());
            return out;
        }

        @Override
        public <P, C> void add(OWLReferences ontology, OWLClass instance, P property, int cardinality, C range) {
            ontology.addMinObjectClassExpression(instance, (OWLObjectProperty) property, cardinality, (OWLClass) range);
        }

        @Override
        public <P, C> void remove(OWLReferences ontology, OWLClass instance, P property, int cardinality, C range) {
            ontology.removeMinObjectClassExpression(instance, (OWLObjectProperty) property, cardinality, (OWLClass) range);
        }
    }

    class MORLink
            implements Semantic.Property<OWLReferences,OWLNamedIndividual,MORAxiom.MORLinked>{

        private MORAxiom.MORLinked literals = new MORAxiom.MORLinked();

        public MORLink(){
        }
        public MORLink( OWLObjectProperty property, OWLNamedIndividual value){
            this.literals.setProperty( property);
            this.literals.setValue( value);
        }

        @Override
        public void set(MORAxiom.MORLinked literals) {
            this.literals = literals;
        }

        @Override
        public MORAxiom.MORLinked get() {
            return literals;
        }

        @Override
        public MORAxiom.MORLinked query(OWLReferences ontology, OWLNamedIndividual instance) {
            OWLNamedIndividual value = ontology.getOnlyObjectPropertyB2Individual( instance, literals.getProperty());
            return new MORAxiom.MORLinked( literals.getProperty(), value);
        }

        @Override
        public <P, V> void add(OWLReferences ontology, OWLNamedIndividual instance, P property, V value) {
            ontology.addObjectPropertyB2Individual( instance, (OWLObjectProperty) property, (OWLNamedIndividual) value);
        }

        @Override
        public <P, V> void remove(OWLReferences ontology, OWLNamedIndividual instance, P property, V value) {
            ontology.removeObjectPropertyB2Individual( instance, (OWLObjectProperty) property, (OWLNamedIndividual) value);
        }
    }

    class MORLinks
            implements Semantic.MultiProperty<OWLReferences,OWLNamedIndividual,MORAxiom.MORMultiLinked>{

        MORAxiom.MORMultiLinked links = new MORAxiom.MORMultiLinked(v);

        public MORLinks(){
        }

        @Override
        public void set(MORAxiom.MORMultiLinked links) {
            this.links = links;
        }

        @Override
        public MORAxiom.MORMultiLinked get() {
            return links;
        }


        @Override
        public MORAxiom.MORMultiLinked query(OWLReferences ontology, OWLNamedIndividual instance) {
            //ontology.setOWLEnquirerIncludesInferences( false);
            Set<OWLEnquirer.ObjectPropertyRelations> values = ontology.getObjectPropertyB2Individual(instance);
            //ontology.setOWLEnquirerIncludesInferences( true);
            MORAxiom.MORMultiLinked links = new MORAxiom.MORMultiLinked(v);
            for ( OWLEnquirer.ObjectPropertyRelations r : values)
                links.add( r.getProperty(), r.getValues());
            return links;
        }

        @Override
        public <P,V> void add(OWLReferences ontology, OWLNamedIndividual instance, P property, V value) {
            ontology.addObjectPropertyB2Individual(instance,
                    (OWLObjectProperty) property,(OWLNamedIndividual) value);
        }
        @Override
        public <P,V> void remove(OWLReferences ontology, OWLNamedIndividual instance, P property, V value) {
            ontology.removeObjectPropertyB2Individual(instance,
                    (OWLObjectProperty) property,(OWLNamedIndividual) value);
        }
    }

    class MORLiteral
            implements Semantic.Property<OWLReferences,OWLNamedIndividual,MORAxiom.MORLiteralValue>{

        private MORAxiom.MORLiteralValue literals = new MORAxiom.MORLiteralValue();

        public MORLiteral(){
        }
        public MORLiteral(OWLDataProperty property, OWLLiteral value){
            this.literals.setProperty( property);
            this.literals.setValue( value);
        }

        @Override
        public void set(MORAxiom.MORLiteralValue literals) {
            this.literals = literals;
        }

        @Override
        public MORAxiom.MORLiteralValue get() {
            return literals;
        }


        @Override
        public MORAxiom.MORLiteralValue query(OWLReferences ontology, OWLNamedIndividual instance) {
            //ontology.setOWLEnquirerIncludesInferences( false);
            OWLLiteral value = ontology.getOnlyDataPropertyB2Individual( instance, literals.getProperty());
            //ontology.setOWLEnquirerIncludesInferences( true);
            return new MORAxiom.MORLiteralValue( literals.getProperty(), value);
        }

        @Override
        public <P, V> void add(OWLReferences ontology, OWLNamedIndividual instance, P property, V value) {
            ontology.addDataPropertyB2Individual( instance, (OWLDataProperty) property, (OWLLiteral) value);
        }

        @Override
        public <P, V> void remove(OWLReferences ontology, OWLNamedIndividual instance, P property, V value) {
            ontology.removeDataPropertyB2Individual( instance, (OWLDataProperty) property, (OWLLiteral) value);
        }
    }

    class MORData3D
            implements Semantic.Property3D<OWLReferences,OWLNamedIndividual,MORAxiom.MORLiteralValue3D>{

        private MORAxiom.MORLiteralValue3D link3D;

        public MORData3D(){
            link3D = new MORAxiom.MORLiteralValue3D();
        }
        public MORData3D(OWLReferences onto, String prefix, String xSuff, String ySuff, String zSuff){
            link3D = new MORAxiom.MORLiteralValue3D( onto, prefix, xSuff, ySuff, zSuff);
        }

        @Override
        public void set(MORAxiom.MORLiteralValue3D atom) {
            link3D = atom;
        }

        @Override
        public MORAxiom.MORLiteralValue3D get() {
            return link3D;
        }


        @Override
        public MORAxiom.MORLiteralValue3D query(OWLReferences ontology, OWLNamedIndividual instance) {
            MORAxiom.MORLiteralValue3D queriedLink = new MORAxiom.MORLiteralValue3D();
            queriedLink.getX().setProperty( link3D.getX().getProperty());
            queriedLink.getX().setValue(
                    ontology.getOnlyDataPropertyB2Individual( instance, queriedLink.getX().getProperty()));
            queriedLink.getY().setProperty( link3D.getY().getProperty());
            queriedLink.getY().setValue(
                    ontology.getOnlyDataPropertyB2Individual( instance, queriedLink.getY().getProperty()));
            queriedLink.getZ().setProperty( link3D.getZ().getProperty());
            queriedLink.getZ().setValue(
                    ontology.getOnlyDataPropertyB2Individual( instance, queriedLink.getZ().getProperty()));
            return queriedLink;
        }

        private <P,V> void add(OWLReferences ontology, OWLNamedIndividual instance, P property, V value) {
            ontology.addDataPropertyB2Individual( instance, (OWLDataProperty) property, (OWLLiteral) value);
        }
        private <P,V> void remove(OWLReferences ontology, OWLNamedIndividual instance, P property, V value) {
            ontology.removeDataPropertyB2Individual( instance, (OWLDataProperty) property, (OWLLiteral) value);
        }

        @Override
        public <P, V> void addX(OWLReferences ontology, OWLNamedIndividual instance, P property, V value) {
            add(ontology,instance,property,value);
        }

        @Override
        public <P, V> void addY(OWLReferences ontology, OWLNamedIndividual instance, P property, V value) {

        }

        @Override
        public <P, V> void addZ(OWLReferences ontology, OWLNamedIndividual instance, P property, V value) {

        }

        @Override
        public <P, V> void removeX(OWLReferences ontology, OWLNamedIndividual instance, P property, V value) {

        }

        @Override
        public <P, V> void removeY(OWLReferences ontology, OWLNamedIndividual instance, P property, V value) {

        }

        @Override
        public <P, V> void removeZ(OWLReferences ontology, OWLNamedIndividual instance, P property, V value) {

        }
    }
    */
}
