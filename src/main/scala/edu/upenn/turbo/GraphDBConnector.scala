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

import java.nio.file.Path
import java.nio.file.Paths
import org.slf4j.LoggerFactory

class GraphDBConnector 
{
    val logger = LoggerFactory.getLogger(getClass)

    def getDiseaseURIs(startingCodes: Array[String], cxn: RepositoryConnection): Array[Array[String]] =
    {
        var startListAsString = ""
        for (code <- startingCodes) startListAsString += " <" + code + "> "

        val query = """
            PREFIX obo: <http://purl.obolibrary.org/obo/>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX owl: <http://www.w3.org/2002/07/owl#>
            PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
            PREFIX snomed: <http://purl.bioontology.org/ontology/SNOMEDCT/>
            PREFIX umls: <http://bioportal.bioontology.org/ontologies/umls/>
            PREFIX oboInOwl: <http://www.geneontology.org/formats/oboInOwl#>
            PREFIX turbo: <http://transformunify.org/ontologies/>

            select
            distinct ?icdsub ?mondosub ?mlab2
            where {
                 values ?icdsub {
                     """ + startListAsString + """
                 }
                graph obo:mondo.owl {
                    #      ?mondostart rdfs:label ?mlab1 .
                    #      ?mondosub rdfs:subClassOf* ?mondostart ;
                    #                               rdfs:label ?mlab2 .
                    ?mondosub rdfs:label ?mlab2 .
                }
                graph <http://graphBuilder.org/mondoToIcdMappings>
                {
                    ?mondosub <http://graphBuilder.org/mapsTo> ?icdsub .
                }
            }"""

        val tupleQueryResult = cxn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()
        val resultList: ArrayBuffer[Array[String]] = new ArrayBuffer[Array[String]]
        while (tupleQueryResult.hasNext()) 
        {
            val bindingset: BindingSet = tupleQueryResult.next()
            var icdSub: String = bindingset.getValue("icdsub").toString
            var mondoSub: String = bindingset.getValue("mondosub").toString
            var mondoLabel: String = bindingset.getValue("mlab2").toString
            logger.info(icdSub + " " + mondoSub + " " + mondoLabel)
            resultList += Array(icdSub, mondoSub, mondoLabel)
        }
        logger.info("result size: " + resultList.size)
        resultList.toArray
    }

    def getDiagnosisCodes(start: String, cxn: RepositoryConnection): Array[String] =
    {
        val query = """
            PREFIX obo: <http://purl.obolibrary.org/obo/>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX owl: <http://www.w3.org/2002/07/owl#>
            PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
            PREFIX snomed: <http://purl.bioontology.org/ontology/SNOMEDCT/>
            PREFIX umls: <http://bioportal.bioontology.org/ontologies/umls/>
            PREFIX oboInOwl: <http://www.geneontology.org/formats/oboInOwl#>
            PREFIX turbo: <http://transformunify.org/ontologies/>
            select
            distinct ?icdsub
            where {
                values ?mondostart {
                    <"""+start+""">
                }
                graph obo:mondo.owl {
                    ?mondostart rdfs:label ?mlab1 .
                    ?mondosub rdfs:subClassOf* ?mondostart ;
                                             rdfs:label ?mlab2 .
                }
                graph <http://graphBuilder.org/mondoToIcdMappings>
                {
                    ?mondosub <http://graphBuilder.org/mapsTo> ?icdsub .
                }
            }
        """
        val tupleQueryResult = cxn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()
        val resultList: ArrayBuffer[String] = new ArrayBuffer[String]
        while (tupleQueryResult.hasNext()) 
        {
            val bindingset: BindingSet = tupleQueryResult.next()
            var result: String = bindingset.getValue("icdsub").toString
            resultList += result
        }
        logger.info("result size: " + resultList.size)
        resultList.toArray
    }
}