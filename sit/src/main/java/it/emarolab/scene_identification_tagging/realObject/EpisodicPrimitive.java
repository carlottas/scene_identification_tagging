package it.emarolab.scene_identification_tagging.realObject;


import it.emarolab.amor.owlInterface.OWLReferences;
import it.emarolab.scene_identification_tagging.SITBase;
import it.emarolab.scene_identification_tagging.owloopDescriptor.SpatialIndividualDescriptor;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import it.emarolab.scene_identification_tagging.sceneRepresentation.Relation;
import java.util.List;
import java.util.ArrayList;

public class EpisodicPrimitive extends SpatialIndividualDescriptor
        implements SITBase {
    protected static long currentCOUNT = 0;
    private Long time = System.currentTimeMillis();
    private boolean addTime = false;
    private boolean addId = false;
    private String color = null;
    private Long id = null;
    private ArrayList<Relation>relations;
    private String Name = null;

    /**
     * Initialise and ontological object by have a name based on {@link #currentCOUNT}
     * and {@link INDIVIDUAL#PREFIX_PRIMITIVE}.
     * @param onto the {@link OWLReferences} ontology that will contain the object individual.
     */
    public EpisodicPrimitive(OWLReferences onto) {
        super( INDIVIDUAL.PREFIX_PRIMITIVE + currentCOUNT, onto);
        initialiseProperty();
        currentCOUNT += 1;
    }
    /**
     * Initialise and ontological object by have a name based on {@link #currentCOUNT}
     * and {@link INDIVIDUAL#PREFIX_PRIMITIVE}.
     * @param ontoName the name of the {@link OWLReferences} ontology that will contain the object individual.
     */
    public EpisodicPrimitive(String ontoName) {
        super(INDIVIDUAL.PREFIX_PRIMITIVE + currentCOUNT, ontoName);
        initialiseProperty();
        currentCOUNT += 1;
    }
    /**
     * Initialise an object in the ontology by fully describing its OWLOOP {@code Ground}.
     * @param instance the object individual.
     * @param onto the {@link OWLReferences} ontology that will contain the object individual.
     */
    public EpisodicPrimitive(OWLNamedIndividual instance, OWLReferences onto) {
        super(instance, onto);
        initialiseProperty();
        currentCOUNT += 1;
    }
    /**
     * Initialise an object in the ontology by fully describing its OWLOOP {@code Ground}.
     * @param instanceName the name of the object individual.
     * @param onto the {@link OWLReferences} ontology that will contain the object individual.
     */
    public EpisodicPrimitive(String instanceName, OWLReferences onto) {
        super(instanceName, onto);
        initialiseProperty();
        currentCOUNT += 1;
    }
    /**
     * Initialise an object in the ontology by fully describing its OWLOOP {@code Ground}.
     * @param instance the object individual.
     * @param ontoName the name of the {@link OWLReferences} ontology that will contain the object individual.
     */
    public EpisodicPrimitive(OWLNamedIndividual instance, String ontoName) {
        super(instance, ontoName);
        initialiseProperty();
        currentCOUNT += 1;
    }
    /**
     * Initialise an object in the ontology by fully describing its OWLOOP {@code Ground}.
     * @param instanceName the name of the object individual.
     * @param ontoName the name of the {@link OWLReferences} ontology that will contain the object individual.
     */
    public EpisodicPrimitive(String instanceName, String ontoName) {
        super(instanceName, ontoName);
        initialiseProperty();
        currentCOUNT += 1;
    }

    // common constructor initialise
    private void initialiseProperty(){
        addData( getPropertyTime(), time, true);
        addData( getPropertyColor(),  true);
        // add this to avoid error on bug in the SWRL
        addTypeIndividual( CLASS.PRIMITIVE);
    }

    /**
     * Return the name of data property used to map time stamps.
     * @return {@link DATA_PROPERTY#TIME}
     */
    public String getPropertyTime() {
        return DATA_PROPERTY.TIME;
    }

    /**
     * Return the name of data property used to map unique identifier.
     * @return {@link DATA_PROPERTY#ID}
     */
    public String getPropertyId() {
        return DATA_PROPERTY.ID;
    }

    /**
     * return the name of the data property used to define the geometric primitive color
     * @return{@link COLOR#COLOR_DATA_PROPERTY}
     */
    public String getPropertyColor(){
        return COLOR.COLOR_DATA_PROPERTY;
    }

    /**
     * Return the time stamp, by default the construction time
     * in milliseconds.
     * @return the time stamp of the object.
     */
    public Long getTime() {
        return time;
    }

    /**
     * Set the time stamp associated to {@code this} object.
     * if {@link #isAddingTime()}, this data will be added to the
     * {@link SpatialIndividualDescriptor}.
     * @param time the time stamp of the object.
     */
    public void setTime(Long time) { // does not change object name
        this.time = this.time;
        if ( addTime)
            addData( getPropertyTime(), this.time, true);
    }

    /**
     * Return the unique identifier, by default it is {@code null}
     * @return the unique identifier of the object, if is has been set.
     */
    public Long getId() {
        return id;
    }

    /**
     * Set the unique identifier associated to {@code this} object.
     * if {@link #isAddingId()}, this data will be added to the
     * {@link SpatialIndividualDescriptor}.
     * @param id the unique identifier of the object.
     */
    public void setId(Long id) {
        this.id = id;
        if ( addId)
            addData( getPropertyId(), id, true);
    }
    /**
     * Set the {@code color}
     * {@link SpatialIndividualDescriptor}.
     */
    public void setColor(String Color) {
        this.color =Color;
        addData( getPropertyColor(), Color ,true);
    }

    /**Return the color
     *
     */
    public String getColor(){
        return this.color;
    }
    /**
     * Enhance the standard OWLOOP read semantic by
     * explicitly set the value of the center of mass and,
     * eventually the time stamp and unique identifier.
     * @return the changes done during reading.
     */

    public void setRelations(ArrayList<Relation> relations){
        this.relations= relations;
    }

    public ArrayList<Relation> getRelations(){
        return this.relations;
    }

    public void setName(String Name){this.Name=Name;}
    public String getName(){return this.Name; }

    @Override
    public List<MappingIntent> readSemantic() {
        List<MappingIntent> r = super.readSemantic();
        color = getLiteral(getPropertyColor()).getLiteral();
        if( addId)
            id = Long.valueOf( getLiteral( getPropertyId()).getLiteral());
        if( addTime)
            time = Long.valueOf( getLiteral( getPropertyTime()).getLiteral());
        return r;
    }

    /**
     * An adding time enable/disable flag.
     * @return {@code true} if the time stamp is synchronised w.r.t. the ontology.
     * {@code false} otherwise.
     */
    public boolean isAddingTime() {
        return addTime;
    }

    /**
     * An adding time enable/disable flag for synchronising the
     * time stamp w.r.t. the ontology.
     * @param flag {@code true} to enable synchronisation, {@code false} to disable.
     */
    public void shouldAddTime(boolean flag) {
        addTime = flag;
    }

    /**
     * An adding unique identifier enable/disable flag.
     * @return {@code true} if the time stamp is synchronised w.r.t. the ontology.
     * {@code false} otherwise.
     */
    public boolean isAddingId() {
        return addTime;
    }

    /**
     * An adding time enable/disable flag for synchronising the
     * unique identifier w.r.t. the ontology.
     * @param flag {@code true} to enable synchronisation, {@code false} to disable.
     */
    public void shouldAddId(boolean flag) {
        addId = flag;
    }

    public void ApplyRelations () {
        this.readSemantic();
        for (Relation r : relations){
            for(String s : r.getObject()){
                this.addObject(r.getRelation(),s,true);
                System.out.print("Adding Object Property, inside the class");
            }
        }


    }



}
