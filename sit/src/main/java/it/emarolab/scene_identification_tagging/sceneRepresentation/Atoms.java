package it.emarolab.scene_identification_tagging.sceneRepresentation;

import it.emarolab.scene_identification_tagging.sceneRepresentation.Atom;
import sit_msgs.*;
import org.ros.node.ConnectedNode;
import it.emarolab.scene_identification_tagging.Interfaces.MemoryInterface;
import it.emarolab.scene_identification_tagging.sceneRepresentation.Relation;
import java.util.*;


public class Atoms extends HashSet<Atom> implements MemoryInterface{

    /**
     * Map this set into the ROS message returned by this service ({@link sit_test_msgs.SpatialRelation})
     * @param node the bridge to the standard ROS utilities.
     * @param res the service response to be set with the data contained in this set.
     */

    public void mapInROSMsg(ConnectedNode node, SemanticInterfaceResponse res){
        ArrayList<sit_msgs.SpatialAtom> rosAtoms= new ArrayList<sit_msgs.SpatialAtom>();
        for ( Atom s : this){
            sit_msgs.SpatialAtom rosAtom=node.getTopicMessageFactory().newFromType( sit_msgs.SpatialAtom._TYPE);
            ArrayList<sit_msgs.SpatialRelationship> rel= new ArrayList<sit_msgs.SpatialRelationship>();
            for ( Relation r :s.getRelations()){
                //create the relation subject atom

                for(String o:r.getObject()) {
                    sit_msgs.SpatialRelationship rosSR = node.getTopicMessageFactory().newFromType(SpatialRelationship._TYPE);
                    rosSR.setRelation(r.getRelation());
                    rosSR.setObject(r.getObject());
                    rel.add(rosSR);
                }
            }
            rosAtom.setRelations(rel);
            rosAtom.setName(s.getName());
            rosAtom.setColor(s.getColor());
            float[] floatArray = memory.ListToArray(s.getCoefficients());
            rosAtom.setCoefficients(floatArray);
            rosAtom.setType(s.getType());
            rosAtoms.add(rosAtom);

        }
        res.setObjects(rosAtoms);
    }
    public void MapFromRosMsg(List<sit_msgs.SpatialAtom> rosAtoms){

        for(sit_msgs.SpatialAtom a: rosAtoms){
            ArrayList<Relation> rel= new ArrayList<>();
            for (sit_msgs.SpatialRelationship rosSR:a.getRelations()) {
                ArrayList<String> objects= new ArrayList<>();
                for(int i=0; i<rosSR.getObject().size();i++){
                    objects.add(rosSR.getObject().get(i));
                }
                Relation r = new Relation(objects,rosSR.getRelation());
                rel.add(r);
            }
            ArrayList<Float> coefficients=new ArrayList<>();
            for ( int i = 0; i<a.getCoefficients().length;i++){
                coefficients.add(a.getCoefficients()[i]);

            }
            this.add(new Atom(a.getName(),a.getType(),a.getColor(),coefficients,rel));
        }
    }


    @Override
    public boolean add(Atom atom) {
        // simplify the list by not adding redundant relation
        for ( Atom a : this)
            if ( a.equals(atom))
                return false;
        // add the relation
        return super.add(atom);
    }

    /**
     * @return the textual description of this set.
     */
    @Override
    public String toString() {
        String out = "\n{";
        int cnt = 0;
        for ( Atom s : this) {
            out += "\t" + s.toString();
            if( ++cnt < this.size())
                out += ";\n";
        }
        return out + "}";
    }
}
