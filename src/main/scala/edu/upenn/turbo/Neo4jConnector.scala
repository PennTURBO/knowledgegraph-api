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
import scala.collection.mutable.HashMap

import java.nio.file.Path
import java.nio.file.Paths
import org.slf4j.LoggerFactory

class Neo4jConnector 
{
    val logger = LoggerFactory.getLogger(getClass)
    def getOrderNames(mappedTerm: String, g: GraphTraversalSource): Array[String] =
    {
        logger.info("Starting graph traversal")
        val result = g.V().has("uri", mappedTerm).
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
            "http://purl.bioontology.org/ontology/RXNORM/contains",
            "http://graphBuilder.org/materialized_drug_relationship",
            "http://graphBuilder.org/materialized_chebi_toRxnorm_relationship",
            "http://graphBuilder.org/materialized_chebi_subclass_relationship"
            ).simplePath()).emit().times(5).dedup().
        in("http://example.com/resource/rxnifavailable").has("http://example.com/resource/FULL_NAME").
        dedup().toList().toArray
        logger.info("Finished graph traversal")
        //logger.info("gremlin result size: " + result.size)
        var buffToSet = new ArrayBuffer[String]
        for (a <- result) buffToSet += a.asInstanceOf[org.apache.tinkerpop.gremlin.structure.Vertex].
            value("http://example.com/resource/FULL_NAME").toString
        logger.info("collected results in array")
        val finalList = buffToSet.toSet.toArray
        logger.info("final size: " + finalList.size)
        finalList
    }

    def getHopsAwayFromDrug(startTerm: Array[String], g: GraphTraversalSource): Map[String, Array[String]] =
    {
        var res = new HashMap[String, Array[String]]
        logger.info("received input of size: " + startTerm.size)
        for (a <- startTerm)
        {
            logger.info("scanning uri: " + a)
            val result = g.V().has("uri", a).
            repeat(out("http://www.w3.org/2000/01/rdf-schema#subClassOf").simplePath()).emit()
            .times(7).has("uri", "http://purl.obolibrary.org/obo/CHEBI_23888")/*.until(has("uri", "http://purl.obolibrary.org/obo/CHEBI_23888"))*/
            .path().by("uri").toList.toArray

            var buffToSet = new ArrayBuffer[String]
            for (b <- result) buffToSet += b.toString
            logger.info("collected results in array")
            val finalList = buffToSet.toArray
            logger.info("final size: " + finalList.size)

            res += a -> finalList
        }
        res.toMap
    }
}