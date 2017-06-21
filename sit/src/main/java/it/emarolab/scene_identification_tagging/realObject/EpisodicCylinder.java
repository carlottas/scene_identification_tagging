package it.emarolab.scene_identification_tagging.realObject;

import it.emarolab.amor.owlInterface.OWLReferences;
import it.emarolab.scene_identification_tagging.SITBase;
import it.emarolab.scene_identification_tagging.owloopDescriptor.SpatialIndividualDescriptor;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.util.List;

public class EpisodicCylinder
        extends EpisodicPrimitive
        implements SITBase{

    private Float height = null;
    private Float radius = null;

    /**
     * Initialise and ontological object by have a name based on {@link #currentCOUNT}
     * and {@link INDIVIDUAL#PREFIX_CYLINDER}.
     * @param onto the {@link OWLReferences} ontology that will contain the object individual.
     */
    public EpisodicCylinder(OWLReferences onto) {
        super( INDIVIDUAL.PREFIX_CYLINDER + currentCOUNT, onto);
        initialiseProperty();
    }
    /**
     * Initialise and ontological object by have a name based on {@link #currentCOUNT}
     * and {@link INDIVIDUAL#PREFIX_CYLINDER}.
     * @param ontoName the name of the {@link OWLReferences} ontology that will contain the object individual.
     */
    public EpisodicCylinder(String ontoName) {
        super(INDIVIDUAL.PREFIX_CYLINDER + currentCOUNT, ontoName);
        initialiseProperty();
    }
    /**
     * Initialise an object in the ontology by fully describing its OWLOOP {@code Ground}.
     * @param instance the object individual.
     * @param onto the {@link OWLReferences} ontology that will contain the object individual.
     */
    public EpisodicCylinder(OWLNamedIndividual instance, OWLReferences onto) {
        super(instance, onto);
        initialiseProperty();
    }
    /**
     * Initialise an object in the ontology by fully describing its OWLOOP {@code Ground}.
     * @param instanceName the name of the object individual.
     * @param onto the {@link OWLReferences} ontology that will contain the object individual.
     */
    public EpisodicCylinder(String instanceName, OWLReferences onto) {
        super(instanceName, onto);
        initialiseProperty();
    }
    /**
     * Initialise an object in the ontology by fully describing its OWLOOP {@code Ground}.
     * @param instance the object individual.
     * @param ontoName the name of the {@link OWLReferences} ontology that will contain the object individual.
     */
    public EpisodicCylinder(OWLNamedIndividual instance, String ontoName) {
        super(instance, ontoName);
        initialiseProperty();
    }
    /**
     * Initialise an object in the ontology by fully describing its OWLOOP {@code Ground}.
     * @param instanceName the name of the object individual.
     * @param ontoName the name of the {@link OWLReferences} ontology that will contain the object individual.
     */
    public EpisodicCylinder(String instanceName, String ontoName) {
        super(instanceName, ontoName);
        initialiseProperty();
    }

    // common constructor
    private void initialiseProperty(){
        addData( getPropertyHeight(), true);
        addData( getPropertyRadius(), true);
        addTypeIndividual( CLASS.CYLINDER);
        System.out.println("INITIALIZE THE PROPERY");

    }



    /**
     * Return the data property used to map the height of the cylinder.
     * @return {@link DATA_PROPERTY#CYLINDER_HEIGHT}
     */
    public String getPropertyHeight(){
        return DATA_PROPERTY.CYLINDER_HEIGHT;
    }

    /**
     * Return the data property used to map the height of the cylinder.
     * @return {@link DATA_PROPERTY#CYLINDER_RADIUS}
     */
    public String getPropertyRadius(){
        return DATA_PROPERTY.CYLINDER_RADIUS;
    }


    /**
     * Returns the height value of the cylinder.
     * Measurements should be in meters.
     * @return the height.
     */
    public Float getHight() {
        return height;
    }

    /**
     * Set the height value of the cylinder.
     * Measurements should be in meters.
     * This method automatically add the data to the
     * {@link SpatialIndividualDescriptor}.
     * @param h the height value.
     */
    public void setHeight(Float h) {
        this.height = h;
        addData( getPropertyHeight(), h, true);
        if(height>FIVE && height<TEN){
            addTypeIndividual(CLASS.CYLINDER_HEIGHT_INCLUDED_IN_TEN_FIVE);
        }
        else if(height<=FIVE){
            addTypeIndividual(CLASS.CYLINDER_HEIGHT_SMALLER_THAN_FIVE);
        }
        else if (height>=TEN){
            addTypeIndividual(CLASS.CYLINDER_HEIGHT_BIGGER_THAN_TEN);
        }
    }

    /**
     * Returns the radius value of the cylinder.
     * Measurements should be in meters.
     * @return the radius.
     */
    public Float getRadius() {
        return radius;
    }

    /**
     * Set the radius value of the cylinder.
     * Measurements should be in meters.
     * This method automatically add the data to the
     * {@link SpatialIndividualDescriptor}.
     * @param r the radius value.
     */
    public void setRadius(Float r) {
        this.radius = r;
        addData( getPropertyRadius(), r, true);
        if(radius>FIVE && radius<TEN){
            addTypeIndividual(CLASS.CYLINDER_RADIUS_INCLUDED_IN_TEN_FIVE);
        }
        else if(radius<=FIVE){
            addTypeIndividual(CLASS.CYLINDER_RADIUS_SMALLER_THAN_FIVE);
        }
        else if (radius>=TEN){
            addTypeIndividual(CLASS.CYLINDER_RADIUS_BIGGER_THAN_TEN);
        }
    }

    /**
     * Enhance the standard OWLOOP read semantic by
     * explicitly set the value of a point in the axis, the height and the radius
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

    @Override
    public boolean equals(Object o ){
        if(this == o) return true;
        if (!(o instanceof  EpisodicCylinder)) return false;

        EpisodicCylinder that= (EpisodicCylinder) o;
        if(this.getTypeIndividual().toString().equals(that.getTypeIndividual().toString())) return true;
        else return false;
    }
}
