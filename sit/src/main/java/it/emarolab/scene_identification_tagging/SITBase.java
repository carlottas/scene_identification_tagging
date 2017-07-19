package it.emarolab.scene_identification_tagging;

/**
 * The base interface for the SIT architecture.
 * <p>
 *     This interface is aimed to be a container for static methods
 *     and constants, as well as logging facility.
 *     It should be implemented by all classes of this architecture
 *
 * <div style="text-align:center;"><small>
 * <b>File</b>:        it.emarolab.scene_identification_tagging.SITBase <br>
 * <b>Licence</b>:     GNU GENERAL PUBLIC LICENSE. Version 3, 29 June 2007 <br>
 * <b>Author</b>:      Buoncompagni Luca (luca.buoncompagni@edu.unige.it) <br>
 * <b>affiliation</b>: EMAROLab, DIBRIS, University of Genoa. <br>
 * <b>date</b>:        05/06/17 <br>
 * </small></div>
 */
public interface SITBase {

    /**
     * The threshold of the recognition confidence {@code [0,1]}
     * to consider a scene different enough (from the best recognition)
     * to be learned.
     */
    double CONFIDENCE_THRESHOLD = .8;

    /**
     * Value for differentiate between different geometric features
     */
    float FIVE=(float)0.05;
    float TEN=(float) 0.10 ;

    /**
     * The path to the main t-box ontological representation
     * used by the SIT algorithm, with respect to the {@code src} folder.
     */
    String ONTO_FILE = "../catkin_ws/src/scene_identification_tagging/resources/t_box/empty-scene.owl";
    String EPISODIC_ONTO_FILE = "../catkin_ws/src/scene_identification_tagging/resources/t_box/episodic-onto.owl";

    /**
     * The {@code IRI} domain of the main t-box ontological representation
     * used by the SIT algorithm.
     */
    String ONTO_IRI = "http://www.semanticweb.org/emaroLab/luca-buoncompagni/sit";
    String EPISODIC_ONTO_IRI = "http://www.semanticweb.org/emaroLab/luca-buoncompagni/sit";
    String EPISODIC_ONTO_NAME="Episodic";
    /**
     * The base interface for the SIT constants.
     * <p>
     *     This interface is aimed to be a container for all the
     *     {@code String} constants used in the ontology.
     *     It is extended in order to give a clear semantic to
     *     the constants by a name space.
     *     <br>
     *     Those names are tuned with respect to the t-box
     *     ontology representation used by the SIT algorithm
     *     (available at: {@link #ONTO_FILE})
     *
     * <div style="text-align:center;"><small>
     * <b>File</b>:        it.emarolab.scene_identification_tagging.SITBase <br>
     * <b>Licence</b>:     GNU GENERAL PUBLIC LICENSE. Version 3, 29 June 2007 <br>
     * <b>Author</b>:      Buoncompagni Luca (luca.buoncompagni@edu.unige.it) <br>
     * <b>affiliation</b>: EMAROLab, DIBRIS, University of Genoa. <br>
     * <b>date</b>:        05/06/17 <br>
     * </small></div>
     */
    interface VOCABOLARY {

        /**
         * The prefix used for data properties as well
         * as for object properties that relate a generic
         * object to the abstract {@code Scene} representation.
         */
        String PREFIX_HAS = "has-";

        /**
         * The prefix used for all the data properties used
         * to describe geometric coefficients of objects.
         */
        String PREFIX_GEOMETRIC = PREFIX_HAS + "geometric_";

        /**
         * The suffix used for all the data and object properties that
         * are related to the {@code X} axis.
         */
        String SUFFIX_X = "X";

        /**
         * The suffix used for all the data and object properties that
         * are related to the {@code Y} axis.
         */
        String SUFFIX_Y = "Y";

        /**
         * The suffix used for all the data and object properties that
         * are related to the {@code Z} axis.
         */
        String SUFFIX_Z = "Z";
    }

    /**
     * The interface containing all the constants related to individuals.
     * <p>
     *     This interface is aimed to be a container for all the
     *     {@code String} constants used in the ontology
     *     for addressing individuals.
     *
     * <div style="text-align:center;"><small>
     * <b>File</b>:        it.emarolab.scene_identification_tagging.SITBase <br>
     * <b>Licence</b>:     GNU GENERAL PUBLIC LICENSE. Version 3, 29 June 2007 <br>
     * <b>Author</b>:      Buoncompagni Luca (luca.buoncompagni@edu.unige.it) <br>
     * <b>affiliation</b>: EMAROLab, DIBRIS, University of Genoa. <br>
     * <b>date</b>:        05/06/17 <br>
     * </small></div>
     */
    interface INDIVIDUAL
            extends VOCABOLARY{


        /**
         * The prefix used for all the individuals with
         * a primitive shape (i.e.: belonging to
         * the {@link CLASS#PRIMITIVE}).
         */
        String PREFIX_PRIMITIVE = "P-";

        /**
         * The prefix used for all the individuals with
         * a spherical shape (i.e.: belonging to
         * the {@link CLASS#SPHERE}).
         */
        String PREFIX_SPHERE = "S-";

        /**
         * The prefix used for all the individuals with
         * shape that has an orientation (i.e.: belonging to
         * the {@link CLASS#ORIENTABLE}).
         */
        String PREFIX_ORIENTATABLE = "O-";

        /**
         * The prefix used for all the individuals with
         * a conical shape (i.e.: belonging to
         * the {@link CLASS#CONE}).
         */
        String PREFIX_CONE = "C-";

        /**
         * The prefix used for all the individuals with
         * a cylindrical shape (i.e.: belonging to
         * the {@link CLASS#CYLINDER}).
         */
        String PREFIX_CYLINDER = "R-";

        /**
         * The prefix used for all the individuals with
         * a planar shape (i.e.: belonging to
         * the {@link CLASS#PLANE}).
         */
        String PREFIX_PLANE = "P-";

        /**
         * The prefix used for all the individuals
         * that define an abstract scene representation
         * (i.e.: belonging to the {@link CLASS#SPHERE}).
         */
        String SCENE = "Sn-";

        String EPISODIC_SCENE="SnEp-";
    }

    /**
     * The interface containing all the constants related to classes.
     * <p>
     *     This interface is aimed to be a container for all the
     *     {@code String} constants used in the ontology
     *     for addressing classes.
     *
     * <div style="text-align:center;"><small>
     * <b>File</b>:        it.emarolab.scene_identification_tagging.SITBase <br>
     * <b>Licence</b>:     GNU GENERAL PUBLIC LICENSE. Version 3, 29 June 2007 <br>
     * <b>Author</b>:      Buoncompagni Luca (luca.buoncompagni@edu.unige.it) <br>
     * <b>affiliation</b>: EMAROLab, DIBRIS, University of Genoa. <br>
     * <b>date</b>:        05/06/17 <br>
     * </small></div>
     */
    interface CLASS
            extends VOCABOLARY{

        /**
         * The name of te class containing all the 
         * abstract scene representations (i.e. {@link INDIVIDUAL#SCENE}).
         */
        String SCENE = "Scene";

        /**
         * The name of the class containing all the object
         * with coefficients related to a sphere
         * (i.e.: {@link it.emarolab.scene_identification_tagging.realObject.Sphere}).
         */
        String SPHERE = "Sphere";

        /**
         * The name of the classes that defines the SPHERE with respect to its radius
         *
         */
        String SPHERE_RADIUS_SMALLER_THAN_FIVE="SphereRadiusSmallerThanFive";
        String SPHERE_RADIUS_INCLUDED_IN_TEN_FIVE="SphereRadiusIncludedInTenFive";
        String SPHERE_RADIUS_BIGGER_THAN_TEN="SphereRadiusBiggerThanTen";

        /**
         * The name of the class containing all the object
         * with coefficients related to a plane
         * (i.e.: {@link it.emarolab.scene_identification_tagging.realObject.Plane}).
         */
        String PLANE = "Plane";

        /**
         * The name of the class containing all the object
         * with coefficients related to an orientable sphere
         * (i.e.: {@link it.emarolab.scene_identification_tagging.realObject.Orientable})
         */
        String ORIENTABLE = "Orientable";

        /**
         * The name of the class containing all the object
         * with coefficients related to a cone
         * (i.e.: {@link it.emarolab.scene_identification_tagging.realObject.Cone})
         */
        String CONE = "Cone";
        /**
         * The name of the classes that describe the Cone with respect to its
         * Height and Radius
         */
        String RADIUS="radius";
        String HEIGHT="height";
        String CONE_RADIUS_SMALLER_THAN_FIVE="ConeRadiusSmallerThanFive";
        String CONE_RADIUS_INCLUDED_IN_TEN_FIVE="ConeRadiusIncludedInTenFive";
        String CONE_RADIUS_BIGGER_THAN_TEN="ConeRadiusBiggerThanTen";
        String CONE_HEIGHT_SMALLER_THAN_FIVE="ConeHeightSmallerThanFive";
        String CONE_HEIGHT_INCLUDED_IN_TEN_FIVE="ConeHeightIncludedInTenFive";
        String CONE_HEIGHT_BIGGER_THAN_TEN="ConeHeightBiggerThanTen";
        /**
         * The name of the class containing all the object
         * with coefficients related to a cylinder
         * (i.e.: {@link it.emarolab.scene_identification_tagging.realObject.Cylinder})
         */
        String CYLINDER = "Cylinder";

        /**
         * The name of the classes that describe the Cylinder with respect to its
         * Height and Radius
         */
        String CYLINDER_RADIUS_SMALLER_THAN_FIVE="CylinderRadiusSmallerThanFive";
        String CYLINDER_RADIUS_INCLUDED_IN_TEN_FIVE="CylinderRadiusIncludedInTenFive";
        String CYLINDER_RADIUS_BIGGER_THAN_TEN="CylinderRadiusBiggerThanTen";
        String CYLINDER_HEIGHT_SMALLER_THAN_FIVE="CylinderHeightSmallerThanFive";
        String CYLINDER_HEIGHT_INCLUDED_IN_TEN_FIVE="CylinderHeightIncludedInTenFive";
        String CYLINDER_HEIGHT_BIGGER_THAN_TEN="CylinderHeightBiggerThanTen";
        /**
         * The name of the class containing all the object
         * with coefficients related to a primitive shape
         * (i.e.: {@link it.emarolab.scene_identification_tagging.realObject.GeometricPrimitive})
         */
        String PRIMITIVE = "GeometricPrimitive";
    }

    /**
     * The interface containing all the constants related to data properties.
     * <p>
     *     This interface is aimed to be a container for all the
     *     {@code String} constants used in the ontology
     *     for addressing data properties.
     *     <br>
     *     All related values should be expressed in meters.
     *
     * <div style="text-align:center;"><small>
     * <b>File</b>:        it.emarolab.scene_identification_tagging.SITBase <br>
     * <b>Licence</b>:     GNU GENERAL PUBLIC LICENSE. Version 3, 29 June 2007 <br>
     * <b>Author</b>:      Buoncompagni Luca (luca.buoncompagni@edu.unige.it) <br>
     * <b>affiliation</b>: EMAROLab, DIBRIS, University of Genoa. <br>
     * <b>date</b>:        05/06/17 <br>
     * </small></div>
     */
    interface DATA_PROPERTY
            extends VOCABOLARY{

        /**
         * The name prefix of the data properties used to define the
         * center of mass of a {@link CLASS#PRIMITIVE}.
         */
        String CENTER = PREFIX_GEOMETRIC + "center";
        
        /**
         * The name of the data property defining the {@code X}
         * coordinate of the center of mass of each objects.
         */
        String CENTER_X = CENTER + SUFFIX_X;
        
        /**
         * The name of the data property defining the {@code Y}
         * coordinate of the center of mass of each objects.
         */
        String CENTER_Y = CENTER + SUFFIX_Y;
        
        /**
         * The name of the data property defining the {@code Z}
         * coordinate of the center of mass of each objects.
         */
        String CENTER_Z = CENTER + SUFFIX_Z;

        
        
        /**
         * The name prefix of the data properties used to define the
         * direction of the principal axis of an {@link CLASS#ORIENTABLE}.
         */
        String AXIS = PREFIX_GEOMETRIC + "axis";
        
        /**
         * The name of the data property defining the {@code X}
         * component of the principal direction of each orientable objects.
         */
        String AXIS_X = AXIS + SUFFIX_X;
        
        /**
         * The name of the data property defining the {@code Y}
         * component of the principal direction of each orientable objects.
         */
        String AXIS_Y = AXIS + SUFFIX_Y;
        
        /**
         * The name of the data property defining the {@code Z}
         * component of the principal direction of each orientable objects.
         */
        String AXIS_Z = AXIS + SUFFIX_Z;

        
        
        /**
         * The name of the data property used to define the
         * radius of a {@link CLASS#SPHERE}.
         */
        String RADIUS_SPHERE = PREFIX_HAS + "sphere_radius";

        
        
        /**
         * The name of the data property used to define the
         * hessian of a {@link CLASS#PLANE}.
         */
        String HESSIAN = PREFIX_GEOMETRIC + "hessian";

        
        
        /**
         * The name prefix of the data properties used to define the
         * coefficient specific to a {@link CLASS#CONE}.
         */
        String CONE = "cone_";

        /**
         * The name prefix of the data properties used to define the
         * 3D points of a {@link CLASS#CONE}.
         */
        String APEX = PREFIX_HAS + CONE + "apex";

        /**
         * The name of the data property used to define the {@code X}
         * coordinate of the apex of a {@link CLASS#CONE}.
         */
        String CONE_APEX_X = APEX + SUFFIX_X;

        /**
         * The name of the data property used to define the {@code Y}
         * coordinate of the apex of a {@link CLASS#CONE}.
         */
        String CONE_APEX_Y = APEX + SUFFIX_Y;

        /**
         * The name of the data property used to define the {@code Z}
         * coordinate of the apex of a {@link CLASS#CONE}.
         */
        String CONE_APEX_Z = APEX + SUFFIX_Z;

        /**
         * The name of the data property used to define 
         * the height of a {@link CLASS#CONE}.
         */
        String CONE_HEIGHT = PREFIX_HAS + CONE + "height";

        /**
         * The name of the data property used to define 
         * the radius of a {@link CLASS#CONE}.
         */
        String CONE_RADIUS = PREFIX_HAS + CONE + "radius";


        /**
         * The name prefix of the data properties used to define the
         * coefficient specific to a {@link CLASS#CYLINDER}.
         */
        String CYLINDER = "cylinder_";

        /**
         * The name prefix of the data properties used to define the
         * 3D points of a {@link CLASS#CYLINDER}.
         */
        String CYLINDER_POINT = PREFIX_HAS + CYLINDER + "point";

        /**
         * The name of the data property used to define the {@code X}
         * coordinate of a generic point in the axis of a {@link CLASS#CYLINDER}.
         */
        String CYLINDER_POINT_X = CYLINDER_POINT + SUFFIX_X;

        /**
         * The name of the data property used to define the {@code Y}
         * coordinate of a generic point in the axis of a {@link CLASS#CYLINDER}.
         */
        String CYLINDER_POINT_Y = CYLINDER_POINT + SUFFIX_Y;

        /**
         * The name of the data property used to define the {@code Z}
         * coordinate of a generic point in the axis of a {@link CLASS#CYLINDER}.
         */
        String CYLINDER_POINT_Z = CYLINDER_POINT + SUFFIX_Z;

        /**
         * The name of the data property used to define 
         * the height of a {@link CLASS#CYLINDER}.
         */
        String CYLINDER_HEIGHT = PREFIX_HAS + CYLINDER + "height";

        /**
         * The name of the data property used to define 
         * the radius of a {@link CLASS#CYLINDER}.
         */
        String CYLINDER_RADIUS = PREFIX_HAS + CYLINDER + "radius";

        /**
         * The name of the data property used to define the
         * time stamp of each individuals in the ontology.
         */
        String TIME = PREFIX_HAS + "time";

        /**
         * The name of the data property used to define the
         * unique identifier (with also {@code PREFIX}) of each individuals in the ontology.
         */
        String ID = PREFIX_HAS + "id";

        String BELONG_TO_SCENE="BelongToScene";



    }

    /**
     * The interface containing all the constants related to object properties.
     * <p>
     *     This interface is aimed to be a container for all the
     *     {@code String} constants used in the ontology
     *     for addressing object properties.
     *     <br>
     *     Note that the implementation does not hard code the type
     *     of spatial relations. Those are obtained at run time
     *     by considering all the object properties that are used between
     *     two individual that belongs to {@link CLASS#PRIMITIVE} or its
     *     sub classes.
     *
     * <div style="text-align:center;"><small>
     * <b>File</b>:        it.emarolab.scene_identification_tagging.SITBase <br>
     * <b>Licence</b>:     GNU GENERAL PUBLIC LICENSE. Version 3, 29 June 2007 <br>
     * <b>Author</b>:      Buoncompagni Luca (luca.buoncompagni@edu.unige.it) <br>
     * <b>affiliation</b>: EMAROLab, DIBRIS, University of Genoa. <br>
     * <b>date</b>:        05/06/17 <br>
     * </small></div>
     */
    interface OBJECT_PROPERTY
            extends VOCABOLARY{

        /**
         * The prefix used to define the spatial relations
         * between an abstract scene representation (i.e. {@link INDIVIDUAL#SCENE}).
         * The suffix is composed by the relative object property used between
         * individual identifying real objects, obtained a run time.
         */
        String SCENE_SPATIAL_PRFIX = "has-scene_";

    }
    interface COLOR
    extends  VOCABOLARY{
        /**
         * The name used to identify the different colors
         */
        String YELLOW="yellow";
        String BLUE="blue";
        String GREEN="green";
        String PINK="pink";
        String RED="red";

        /**
         * The name of the data property for geometric primitives related to color
         */
        String COLOR_DATA_PROPERTY= "has-color";
    }
    interface SPATIAL_RELATIONS
    extends VOCABOLARY{
        String PROP_LEFT="isLeftOf";
        String PROP_RIGHT="isRightOf";
        String PROP_PERPENDICULAR="isPerpendicularTo";
        String PROP_PARALLEL="isParallelTo";
        String PROP_IS_IN_FRONT_OF="isInFrontOf";
        String PROP_IS_BEHIND_OF="isBehindOf";
        String PROP_IS_BELOW_OF="isBelowOf";
        String PROP_IS_ABOVE_OF="isAboveOf";
        String PROP_IS_ALONG_X="isAlongX";
        String PROP_IS_ALONG_Y="isAlongY";
        String PROP_IS_ALONG_Z="isAlongZ";
        String PROP_IS_COAXIAL_WITH="isCoaxialWith";
    }

    interface COUNTER
    extends VOCABOLARY{
        String SCENE_COUNTER="Scene-Counter";
        String SPHERE_COUNTER="Sphere-Counter";
        String PLANE_COUNTER="Plane-Counter";
        String CYLINDER_COUNTER="Cylinder-Counter";
        String CONE_COUNTER="Cone-Counter";
        String VALUE_DATA_PROPERTY="has-value";
        String EPISODIC_SCENE_COUNTER="Episodic-Scene-Counter";
    }
    interface GEOMETRICFEATURES
    extends  VOCABOLARY{
        String RADIUS="Radius";
        String HEIGHT="Height";

        String SMALLER_THAN_FIVE="SmallerThanFive";
        String INCLUDED_IN_TEN_FIVE="IncludedInTenFive";
        String BIGGER_THAN_TEN="BiggerThanTen";


    }
    interface SUPPORT
    extends VOCABOLARY{
        String SUPPORT_CLASS_NAME="Support";
        String OBJECT_PROPERTY_IS_SUPPORT_OF="is-support-of_scene";
        String HAS_SCENE_SUPPORT="has-scene_support";
    }
    interface RETRIEVAL
    extends  VOCABOLARY{
        /**
         * name of the class used to do the semantic retrieval
         */
        String SEMANTIC_RETRIEVAL_NAME="retrievalSemantic";
    }
    interface TIME
    extends VOCABOLARY{
        String CLOCK="clock";
        String ONE_HOUR_CLASS="OneHour";
        String TEN_MINUTES_CLASS="TenMinutes";
        String THIRTHY_MINUTES_CLASS="ThirtyMinutes";
        String ONE_HOUR_INDIVIDUAL="OneHour";
        String TEN_MINUTES_INDIVIDUAL="TenMinutes";
        String THIRTY_MINUTES_INDIVIDUAL="ThirtyMinutes";
        String HAS_TIME_CLOCK="has_time";
        String NO_TIME="noTime";
    }
    interface SCORE{
        String SCORE_ONTO_NAME="score-ontology";
        String SCORE_FILE_PATH="../catkin_ws/src/scene_identification_tagging/resources/t_box/carlotta/score-ontology.owl";
        String SCORE_IRI_ONTO="http://www.semanticweb.org/carlotta-sartore/scoreOntology";

        String SCORE_PROP_HAS_TIME="hasTime";
        String SCORE_PROP_HAS_VALUE="hasValue";
        String SCORE_PROP_NUMBER_BELONGING_INDIVIDUAL="NumberBelongingIndividual";
        String SCORE_PROP_NUMBER_EPISODIC_RETRIEVAL="NumberEpisodicRetrieval";
        String SCORE_PROP_NUMBER_RETRIEVAL="NumberRetrieval";
        String SCORE_PROP_NUMBER_SEMANTIC_RETRIEVAL="NumberSemanticRetrieval";
        String SCORE_PROP_NUMBER_SUB_CLASSES="NumberSubClasses";
        String SCORE_PROP_SCORE_SUM_BELONGING_INDIVIDUAL="SumScoreBelongingIndividual";
        String SCORE_PROP_SCORE_BELONGING_INDIVIDUAL="ScoreBelongingIndividual";
        String SCORE_PROP_SCORE_SUM_SUB_CLASSES="SumScoreSubClasses";
        String SCORE_PROP_SCORE_SUB_CLASSES="ScoreSubClasses";
        String SCORE_PROP_TIMES_FORGOTTEN="TimesForgotten";
        String SCORE_PROP_TIMES_TO_BE_FORGOTTEN="TimesForgotten";
        String SCORE_PROP_TIMES_LOW_SCORE="TimesLowScore";
        String SCORE_PROP_USER_NO_FORGET="UserNoForget";
        //Object Property
        String SCORE_PROP_HAS_SCORE="hasScore";
        String SCORE_PROP_IS_SCORE_OF="isScoreOf";
        //Classes
        String SCORE_CLASS_SCENE="Scene";
        String SCORE_CLASS_SCORE="Score";
        String SCORE_CLASS_EPISODIC_SCORE="EpisodicScore";
        String SCORE_CLASS_SEMANTIC_SCORE="SemanticScore";
        String SCORE_CLASS_TOTAL_EPISODIC_SCORE="TotalEpisodicScore";
        String SCORE_CLASS_TOTAL_SEMANTIC_SCORE="TotalSemanticScore";
        String SCORE_CLASS_HIGH_SCORE="ScoreHigh";
        String SCORE_CLASS_EPISODIC_HIGH_SCORE="EpisodicScoreHigh";
        String SCORE_CLASS_SEMANTIC_HIGH_SCORE="SemanticScoreHigh";
        String SCORE_CLASS_LOW_SCORE="ScoreLow";
        String SCORE_CLASS_EPISODIC_LOW_SCORE="EpisodicScoreLow";
        String SCORE_CLASS_SEMANTIC_LOW_SCORE="SemanticScoreLow";
        String SCORE_CLASS_TO_BE_FORGOTTEN="ToBeForgotten";
        String SCORE_CLASS_EPISODIC_TO_BE_FORGOTTEN="EpisodicToBeForgotten";
        String SCORE_CLASS_SEMANTIC_TO_BE_FORGOTTEN="SemanticToBeForgotten";
        String SCORE_CLASS_FORGOTTEN_EPISODIC="EpisodicForgotten";
        String SCORE_CLASS_FORGOTTEN_SEMANTIC="SemanticForgotten";
        String SCORE_OBJ_PROP_FIRST_SUPERCLASS="firstSuperClass";
        String SCORE_OBJ_PROP_IS_FIRST_SUPER_CLASS_OF="isFirstSuperClassOf";
        //individuals
        String SCORE_INDIVIDUAL_TOTAL_EPISODIC="totalEpisodic";
        String SCORE_INDIVIDUAL_TOTAL_SEMANTIC="totalSemantic";
        String SCORE_OBJ_PROP_IS_SUB_CLASS_OF="isSubClassOf";
        String SCORE_OBJ_PROP_IS_SUPER_CLASS_OF="isSuperClassOf";
        String SCORE_OBJ_PROP_IS_INDIVIDUAL_OF="isIndividualOf";
        String SCORE_OBJ_PROP_HAS_INDIVIDUAL="hasIndividual";
        //wheights
        double SCORE_SEMANTIC_WEIGHT_1=0.15;
        double SCORE_SEMANTIC_WEIGHT_2=0.15;
        double SCORE_SEMANTIC_WEIGHT_3=0.15;
        double SCORE_SEMANTIC_WEIGHT_4=0.15;
        double SCORE_SEMANTIC_WEIGHT_5=0.4;
        double SCORE_EPISODIC_WEIGHT_1=0.4;
        double SCORE_EPISODIC_WEIGHT_2=0.6;

    }
    interface FORGETTING{
        String NAME_SEMANTIC_INDIVIDUAL="Forgot";
        String NAME_SEMANTIC_DATA_PROPERTY_FORGOT="toBeForgotten";
        String NAME_DATA_PROPERTY_RETRIEVAL_FORGOT="timesRetrievalForgot";
        float INCREMENT_ONE=(float) 0.1;
        float INCREMENT_TWO=(float) 0.2;
        float INCREMENT_THREE=(float) 0.4;


    }
}
