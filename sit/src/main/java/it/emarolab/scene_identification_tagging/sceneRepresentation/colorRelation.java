package it.emarolab.scene_identification_tagging.sceneRepresentation;

import it.emarolab.scene_identification_tagging.owloopDescriptor.SpatialIndividualDescriptor;

        import com.google.common.base.Objects;
        import it.emarolab.scene_identification_tagging.SITBase;
        import it.emarolab.scene_identification_tagging.owloopDescriptor.SpatialIndividualDescriptor;
        import org.semanticweb.owlapi.model.OWLNamedIndividual;
        import org.semanticweb.owlapi.model.OWLObjectProperty;

public class colorRelation
        implements SITBase{
    private SpatialAtom subject;
    private String color;

    protected colorRelation(SpatialIndividualDescriptor subject,String colorName) {
        this.subject = new SpatialAtom( subject);
        this.color=colorName;
    }
    public SpatialAtom getSubject() {
        return subject;
    }
    public String getColor(){return color; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof colorRelation)) return false;
        colorRelation that = (colorRelation) o;

        return getSubject().equals(that.getSubject())
                & getColor().equals(that.getColor());

    }
    @Override
    public String toString() {
        return subject + "has Color" + color;}

}
