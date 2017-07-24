package it.emarolab.scene_identification_tagging.sceneRepresentation;

import sit_msgs.*;
import it.emarolab.scene_identification_tagging.Interfaces.MemoryInterface;
import it.emarolab.scene_identification_tagging.sceneRepresentation.Relation;
import java.util.*;

public class Atom implements  MemoryInterface {


        private String name;
        private String type;
        private ArrayList<Float> coefficients;
        private String color;
        private ArrayList<Relation> relations;


        public Atom(String name, String type) {
            this.name=name;
            this.type=type;
        }
        public Atom(String name, String type,ArrayList<Float> coefficients) {
            this.name=name;
            this.type=type;
            this.coefficients=coefficients;
        }

        public Atom(String name, String type,String color) {
            this.name=name;
            this.type=type;
            this.color=color;
        }

        public Atom(String name, String type,String color,ArrayList<Float> coefficients) {
            this.name=name;
            this.type=type;
            this.color=color;
            this.coefficients=coefficients;
        }

        public Atom(String name, String type,String color,ArrayList<Float> coefficients, ArrayList<Relation> relations) {
            this.name=name;
            this.type=type;
            this.color=color;
            this.coefficients=coefficients;
            this.relations=relations;
        }

        /**
         * @return the type of this object (described as an ontological class).
         */
        public String getName() {
            return name;
        }

        /**
         * @return the name of this object (described as an ontological individual).
         */
        public String getType() {
            return type;
        }

        public String getColor() {
            return color;
        }
        public ArrayList<Float> getCoefficients() {
            return coefficients;
        }
        public ArrayList<Relation> getRelations() {
            return relations;
        }

        /**
         * Set two {@link Atom}s to be equal if those have the same
         * {@code object} names regardless from the types.
         * @param o the {@link Atom} to test for equality.
         * @return {@code true} if this atom is equal to the given object.
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Atom)) return false;
            Atom that = (Atom) o;
            return getName().equals(that.getName()) &&
                    getRelations().equals(that.getRelations()) &&
                    getColor().equals(that.getColor()) &&
                    getType().equals(that.getType()) ;
        }
        /**
         * It is used to implement {@link #equals(Object)} method.
         * @return a hash code value for this object.
         */
        //TODO
        //@Override
        //public int hashCode() {
        //return getObjectId() != null ? getObjectId().hashCode() : 0;
        // }
        /**
         * @return the textual description of this spatial relation.
         */
        //@Override
        //public String toString() {
        //   return objectId + ":'" + shape + "' ";
        //}



}
