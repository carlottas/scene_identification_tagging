package it.emarolab.scene_identification_tagging.sceneRepresentation;

import java.util.ArrayList;

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
