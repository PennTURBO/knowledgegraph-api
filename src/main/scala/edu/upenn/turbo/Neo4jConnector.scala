package edu.upenn.turbo

import java.io.File
import org.neo4j.graphdb._
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.out
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.in
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.has
import org.apache.tinkerpop.gremlin.process.traversal.P._
import org.eclipse.rdf4j.query.QueryLanguage
import org.eclipse.rdf4j.query.TupleQuery
import org.eclipse.rdf4j.query.TupleQueryResult
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.eclipse.rdf4j.query.BooleanQuery
import org.eclipse.rdf4j.query.BindingSet
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.OpenRDFException
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import scala.collection.mutable.ArrayBuffer

class Neo4jConnector 
{
    def getOrderNames(drugName: String, cxn: RepositoryConnection, g: GraphTraversalSource)
    {
        /*val result = g.V().has("uri", "http://purl.obolibrary.org/obo/CHEBI_35480").
        emit().repeat(in("http://www.w3.org/2000/01/rdf-schema#subClassOf").simplePath()).times(2).dedup().
        in("http://www.w3.org/2002/07/owl#someValuesFrom").in("http://www.w3.org/2000/01/rdf-schema#subClassOf").
        emit().repeat(in("http://www.w3.org/2000/01/rdf-schema#subClassOf").simplePath()).times(2).dedup().
        union(out("http://example.com/resource/materialized_dbxr"),in("http://example.com/resource/inputTerm").as("x").
            out("http://example.com/resource/matchOnt").has("uri", "http://data.bioontology.org/ontologies/RXNORM").
            select("x").out("http://example.com/resource/matchTerm")).dedup().
        repeat(in(
            "http://purl.bioontology.org/ontology/RXNORM/has_ingredient",
            "http://purl.bioontology.org/ontology/RXNORM/isa",
            "http://purl.bioontology.org/ontology/RXNORM/tradename_of",
            "http://purl.bioontology.org/ontology/RXNORM/consists_of",
            "http://purl.bioontology.org/ontology/RXNORM/has_precise_ingredient",
            "http://purl.bioontology.org/ontology/RXNORM/has_ingredients",
            "http://purl.bioontology.org/ontology/RXNORM/has_part",
            "http://purl.bioontology.org/ontology/RXNORM/form_of",
            "http://purl.bioontology.org/ontology/RXNORM/has_form",
            "http://purl.bioontology.org/ontology/RXNORM/contains"
            ).simplePath()).emit().times(3).dedup().
        in("http://example.com/resource/rxnifavailable").has("http://example.com/resource/FULL_NAME").
        dedup().toList()
          
        println("gremlin result size: " + result)*/

        val node1 = g.addV("Resource").property("uri", "node1").next()
        val node2 = g.addV("Resource").property("uri", "node2").next()
        val node3 = g.addV("Resource").property("uri", "node3").next()
        val node4 = g.addV("Resource").property("uri", "node4").next()
        val node5 = g.addV("Resource").property("uri", "node5").next()

        val node6 = g.addV("Resource").property("FULL_NAME", "med1").next()
        val node7 = g.addV("Resource").property("FULL_NAME", "med2").next()
        val node8 = g.addV("Resource").property("FULL_NAME", "med3").next()
        val node9 = g.addV("Resource").property("FULL_NAME", "med4").next()
        val node10 = g.addV("Resource").property("FULL_NAME", "med5").next()

        node1.addEdge("http://graphBuilder.org/materialized_drug_role", node2)
        node2.addEdge("http://purl.bioontology.org/ontology/RXNORM/has_ingredient", node3)
        node3.addEdge("http://purl.bioontology.org/ontology/RXNORM/isa", node4)
        node4.addEdge("http://purl.bioontology.org/ontology/RXNORM/tradename_of", node5)

        node6.addEdge("rxn_if_available", node1)
        node7.addEdge("rxn_if_available", node2)
        node8.addEdge("rxn_if_available", node3)
        node9.addEdge("rxn_if_available", node4)
        node10.addEdge("rxn_if_available", node5)

        val result = g.V().has("uri", "node1").
        repeat(out("http://purl.bioontology.org/ontology/RXNORM/has_ingredient",
            "http://purl.bioontology.org/ontology/RXNORM/isa",
            "http://purl.bioontology.org/ontology/RXNORM/tradename_of",
            "http://purl.bioontology.org/ontology/RXNORM/consists_of",
            "http://purl.bioontology.org/ontology/RXNORM/has_precise_ingredient",
            "http://purl.bioontology.org/ontology/RXNORM/has_ingredients",
            "http://purl.bioontology.org/ontology/RXNORM/has_part",
            "http://purl.bioontology.org/ontology/RXNORM/form_of",
            "http://purl.bioontology.org/ontology/RXNORM/has_form",
            "http://purl.bioontology.org/ontology/RXNORM/contains",
            "http://graphBuilder.org/materialized_drug_role"
            ).simplePath()).emit().times(4).dedup().
        in("rxn_if_available").has("FULL_NAME").
        toList().toArray()

        println("result size: " + result.size)
        for (a <- result) println(a.asInstanceOf[org.apache.tinkerpop.gremlin.structure.Vertex].value("FULL_NAME"))
    }
    
    def querySparql(cxn: RepositoryConnection, query: String): Option[TupleQueryResult] =
    {
        var result: Option[TupleQueryResult] = None : Option[TupleQueryResult]
        try 
        {
            //send input String to Blazegraph SPARQL engine via the RepositoryConnection object
            val tupleQuery: TupleQuery = cxn.prepareTupleQuery(QueryLanguage.SPARQL, query)
            //convert tupleQuery into TupleQueryResult using built-in evaluate() function
            result = Some(tupleQuery.evaluate())
            result
        }
        catch
        {
            case e: OpenRDFException => println(e.toString)
            None
        }
      }
}