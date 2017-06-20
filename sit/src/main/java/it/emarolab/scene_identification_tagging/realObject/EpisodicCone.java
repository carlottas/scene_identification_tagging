package it.emarolab.scene_identification_tagging.realObject;

import it.emarolab.amor.owlInterface.OWLReferences;
import it.emarolab.scene_identification_tagging.SITBase;
import it.emarolab.scene_identification_tagging.owloopDescriptor.SpatialIndividualDescriptor;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.util.List;

public class EpisodicCone
            extends EpisodicPrimitive
            implements SITBase{

        private Float height = null;
        private Float radius = null;

        /**
         * Initialise and ontological object by have a name based on {@link #currentCOUNT}
         * and {@link INDIVIDUAL#PREFIX_CONE}.
         * @param onto the {@link OWLReferences} ontology that will contain the object individual.
         */
        public EpisodicCone(OWLReferences onto) {
            super( INDIVIDUAL.PREFIX_CONE + currentCOUNT, onto);
            initialiseProperty();
        }
        /**
         * Initialise and ontological object by have a name based on {@link #currentCOUNT}
         * and {@link INDIVIDUAL#PREFIX_CONE}.
         * @param ontoName the name of the {@link OWLReferences} ontology that will contain the object individual.
         */
        public EpisodicCone(String ontoName) {
            super(INDIVIDUAL.PREFIX_CONE + currentCOUNT, ontoName);
            initialiseProperty();
        }
        /**
         * Initialise an object in the ontology by fully describing its OWLOOP {@code Ground}.
         * @param instance the object individual.
         * @param onto the {@link OWLReferences} ontology that will contain the object individual.
         */
        public EpisodicCone(OWLNamedIndividual instance, OWLReferences onto) {
            super(instance, onto);
            initialiseProperty();
        }
        /**
         * Initialise an object in the ontology by fully describing its OWLOOP {@code Ground}.
         * @param instanceName the name of the object individual.
         * @param onto the {@link OWLReferences} ontology that will contain the object individual.
         */
        public EpisodicCone(String instanceName, OWLReferences onto) {
            super(instanceName, onto);
            initialiseProperty();
        }
        /**
         * Initialise an object in the ontology by fully describing its OWLOOP {@code Ground}.
         * @param instance the object individual.
         * @param ontoName the name of the {@link OWLReferences} ontology that will contain the object individual.
         */
        public EpisodicCone(OWLNamedIndividual instance, String ontoName) {
            super(instance, ontoName);
            initialiseProperty();
        }
        /**
         * Initialise an object in the ontology by fully describing its OWLOOP {@code Ground}.
         * @param instanceName the name of the object individual.
         * @param ontoName the name of the {@link OWLReferences} ontology that will contain the object individual.
         */
        public EpisodicCone(String instanceName, String ontoName) {
            super(instanceName, ontoName);
            initialiseProperty();
        }

        // common constructor
        private void initialiseProperty(){

            addData( getPropertyHeight(), true);
            addData( getPropertyRadius(), true);
            addTypeIndividual( CLASS.CONE);
        }


        /**
         * Return the data property used to map the height of the code.
         * @return {@link DATA_PROPERTY#CONE_HEIGHT}
         */
        public String getPropertyHeight(){
            return DATA_PROPERTY.CONE_HEIGHT;
        }

        /**
         * Return the data property used to map the radius of the cone.
         * @return {@link DATA_PROPERTY#CONE_RADIUS}
         */
        public String getPropertyRadius(){
            return DATA_PROPERTY.CONE_RADIUS;
        }



        /**
         * Returns the height value of the cone.
         * Measurements should be in meters.
         * @return the height of the cone.
         */
        public Float getHight() {
            return height;
        }

        /**
         * Set the height value of the cone.
         * Measurements should be in meters.
         * This method automatically add the data to the
         * {@link SpatialIndividualDescriptor}.
         * @param h the height value.
         */
        public void setHeight(Float h) {
            this.height = h;
            addData( getPropertyHeight(), h, true);
        }

        /**
         * Returns the radius value of the cone.
         * Measurements should be in meters.
         * @return the radius of the cone.
         */
        public Float getRadius() {
            return radius;
        }

        /**
         * Set the radius value of the cone.
         * Measurements should be in meters.
         * This method automatically add the data to the
         * {@link SpatialIndividualDescriptor}.
         * @param r the radius value.
         */
        public void setRadius(Float r) {
            this.radius = r;
            addData( getPropertyRadius(), r, true);
        }

        /**
         * Enhance the standard OWLOOP read semantic by
         * explicitly set the value of the apex, the height and the radius
         * as well as other measurements from super classes.
         * @return the changes done during reading.
         */
        @Override
        public List<MappingIntent> readSemantic() {
            List<MappingIntent> r = super.readSemantic();
            height = getLiteral( getPropertyHeight()).parseFloat();
            radius = getLiteral( getPropertyRadius()).parseFloat();
            return r;
        }
}
