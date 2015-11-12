package fr.inria.edelweiss.kgraph.approximate.test;

import fr.inria.acacia.corese.api.IDatatype;
import fr.inria.acacia.corese.exceptions.EngineException;
import fr.inria.edelweiss.kgram.api.core.Node;
import fr.inria.edelweiss.kgram.core.Mapping;
import fr.inria.edelweiss.kgram.core.Mappings;
import fr.inria.edelweiss.kgraph.approximate.result.SimilarityResults;
import fr.inria.edelweiss.kgraph.approximate.similarity.Utils;
import fr.inria.edelweiss.kgraph.core.Graph;
import fr.inria.edelweiss.kgraph.query.QueryProcess;
import fr.inria.edelweiss.kgraph.approximate.ext.ASTRewriter;
import static fr.inria.edelweiss.kgraph.approximate.ext.ASTRewriter.MATCH;
import java.util.Arrays;
import java.util.List;

/**
 * Tests.java
 *
 * @author Fuqi Song, Wimmics Inria I3S
 * @date 23 sept. 2015
 */
public class Tests {

    public static void main(String[] ars) throws EngineException {
        Graph g = Graph.create();

        QueryProcess exec = QueryProcess.create(g);
        String init = "insert data {"
                + "<John> rdf:value <testuritest> ;"
                + "rdfs:label 'Johnny'. "
                + "<person1> foaf:name 'John'. "
                + "<person2> foaf:name 'John'. "
                + "<person3> foaf:name 'Johnny'. "
                + "<person4> foaf:name 'John Wang'. "
                + "<person5> foaf:name 'Bruce lee'"
                + ""
                + "<person3> owl:sameAs <person5>"
                + "}";

        String q = ""
                + "select more *"
                + "where {"
                + "kg:jo ?p \"John is a man\" ;"
                + " kg:eat kg:apple "
                + "bind ("
                + "function ( " + MATCH + "(?x, ?y) = "
                + "  contains(?x, ?y) || contains(?y, ?x) "
                + ")"
                + "as ?f )"
                + "} "
                + "pragma { "
                + "kg:approximate kg:algorithm 'eq', 'ss', 'ng'; "
                + "               kg:priority_a '2', '2', '5'; "
                + "               kg:strategy  'URI_NAME', 'URI_LABEL', 'LITERAL_SS', 'PROPERTY_EQUALITY'; "
                + "               kg:priority_s '1', '3', '5', '7'; "
                + "} ";

        String q2 = "PREFIX kg:<http://corese/inria/fr#> "
                + "select *"
                + "where {"
                + "kg:jo ?p <uri> ;"
                + "rdfs:label \"John\" ."
                + "kg:test rdf:type kg:object "
                + "bind ("
                + "function ( " + MATCH + "(?x, ?y) = "
                + "  contains(?x, ?y) || contains(?y, ?x) "
                + ")"
                + "as ?f )"
                + "}";
        String q3 = " "
                + "select more * "
                + "where {"
                + "<person> foaf:name \"John\" ."
                + "} "
                                + "pragma { "
                                + "kg:approximate kg:algorithm 'jw', 'ng'; "
                                + "               kg:priority_a '2', '2'; "
                                + "               kg:strategy  'URI_NAME', 'URI_LABEL', 'LITERAL_LEX'; "
                                + "               kg:priority_s '1', '3', '5'; "
                                + "               kg:wn_path '/Users/fsong/NetBeansProjects/kgram/kgtool/target/classes/wordnet'; "
                                + "               kg:wn_ver '3.0'; "
                                + "               kg:pos_tagger '/Users/fsong/NetBeansProjects/kgram/kgtool/target/classes/tagger/english-left3words-distsim.tagger'; "
                                + "               kg:string_metric 'Lin'; "
                                + "               kg:threshold '0.0'; "
                                + "} "
                + "";

        String q4 = "select * \n"
                + "where\n"
                + "{?person ?_var_0 ?_var_1 . \n"
                + "filter approximate(?_var_0, foaf:name, 'ng-jw-ss-dr') \n"
                + "optional {"
                + "?_var_0 <http://www.w3.org/2002/07/owl#sameAs> foaf:name . \n"
                + "foaf:name <http://www.w3.org/2002/07/owl#sameAs> ?_var_0 . \n"
                + "} \n"
                + "optional {"
                + "?_var_0 <http://www.w3.org/2002/07/owl#equivalentProperty> foaf:name . \n"
                + "foaf:name <http://www.w3.org/2002/07/owl#equivalentProperty> ?_var_0 . \n"
                + "}\n"
                + "filter approximate(?_var_1, 'John', 'ss-ng-jw') }";

        exec.query(init);
        exec.setVisitor(new ASTRewriter());
        Mappings map = exec.query(q3);

        SimilarityResults.aggregate(map);
        //System.out.println("map\n" + map);
        System.out.println(SimilarityResults.getInstance().toString());
    }
}