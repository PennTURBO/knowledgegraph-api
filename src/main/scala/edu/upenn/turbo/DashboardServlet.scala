package edu.upenn.turbo

import org.scalatra._
/*import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json._
import org.json4s._
import org.json4s.JsonDSL._*/
import org.json4s.MappingException

// RDF4J imports
import org.eclipse.rdf4j.rio._
import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.eclipse.rdf4j.query.QueryLanguage
import org.eclipse.rdf4j.model.ValueFactory
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager
import org.eclipse.rdf4j.query.TupleQuery
import org.eclipse.rdf4j.query.TupleQueryResult
import org.eclipse.rdf4j.OpenRDFException
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.eclipse.rdf4j.query.BooleanQuery
import org.eclipse.rdf4j.query.BindingSet
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.rio.RDFFormat
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json._
import com.fasterxml.jackson.core.JsonParseException
import java.util.Properties
import java.io.FileInputStream
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.neo4j.graphdb._
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

import scala.collection.mutable.ArrayBuffer

case class FullNameResults(resultsList: Array[String])
case class FullNameInput(searchTerm: String)

class DashboardServlet extends ScalatraServlet with JacksonJsonSupport 
{
  protected implicit val jsonFormats: Formats = DefaultFormats
  before()
  {
      contentType = formats("json")
  }
  
  post("/medications/findOrderNamesFromInputString")
  {
      println("Received a post request")
      var cxn: RepositoryConnection = null
      var repository: Repository = null
      var repoManager: RemoteRepositoryManager = null
      var neo4jgraph: Neo4jGraph = null
      var parsedResult: String = null

      try 
      { 
          val userInput = request.body
          val extractedResult = parse(userInput).extract[FullNameInput]
          parsedResult = extractedResult.searchTerm

          try
          {
              repoManager = new RemoteRepositoryManager(getFromProperties("serviceURL"))
              repoManager.setUsernameAndPassword(getFromProperties("username"), getFromProperties("password"))
              repoManager.initialize()
              repository = repoManager.getRepository(getFromProperties("repository"))
              cxn = repository.getConnection()
              println("Successfully connected to triplestore")

              try 
              { 
                  neo4jgraph = Neo4jGraph.open("neo4j.graph")
              } 
              catch 
              {
                  case e: Throwable => e.printStackTrace()
              }
              val g: GraphTraversalSource = neo4jgraph.traversal()
              println("Successfully connected to property graph")
            
              val bestResult = getBestMatchTermFromLucene(cxn, parsedResult)
              if (bestResult == None)
              {
                  val noContentMessage = "Your input of \"" + parsedResult + "\" returned no matches."
                  NoContent(Map("message" -> noContentMessage))
              }
              else
              {
                  println("Your input of \"" + parsedResult + "\" was mapped to class " + bestResult.get)
                  val neo4j: Neo4jConnector = new Neo4jConnector
                  FullNameResults(neo4j.getOrderNames(bestResult.get, cxn, g))
              }
          }
          catch
          {
              case e1: JsonParseException => BadRequest(Map("message" -> "Unable to parse JSON"))
          }
          finally
          {
              cxn.close()
              repository.shutDown()
              repoManager.shutDown()
              neo4jgraph.close()
              println("Connections closed.")
          }
      } 
      catch 
      {
          case e1: JsonParseException => BadRequest(Map("message" -> "Unable to parse JSON"))
          case e2: MappingException => BadRequest(Map("message" -> "Unable to parse JSON"))
      }
  }
  
  def getBestMatchTermFromLucene(cxn: RepositoryConnection, userInput: String): Option[String] =
  {
      val query = """
          PREFIX : <http://www.ontotext.com/connectors/lucene#>
          PREFIX inst: <http://www.ontotext.com/connectors/lucene/instance#>
          PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
          PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
          SELECT ?entity ?score ?label {
              ?search a inst:role_via_rdfs_or_skos_label ;
                      :query "role_via_rdfs_label:"""+userInput+""" OR role_via_skos_label:"""+userInput+"""" ;
                                                         :entities ?entity .
              ?entity :score ?score .
              {
                  {
                      graph <http://data.bioontology.org/ontologies/RXNORM/submissions/15/download> 
                      {
                          ?entity skos:prefLabel ?label .
                      }
                  }
                  union
                  {
                      graph <ftp://ftp.ebi.ac.uk/pub/databases/chebi/ontology/chebi_lite.owl.gz>
                      {
                          ?entity rdfs:label ?label .
                      }
                  }
              }
          }
          order by desc(?score)
          limit 1
      """
      val tupleQueryResult = cxn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()
      if (tupleQueryResult.hasNext()) Some(tupleQueryResult.next.getValue("entity").toString)
      else None
  }
  
  def getFromProperties(key: String, file: String = "dashboard.properties"): String =
  {
       val input: FileInputStream = new FileInputStream(file)
       val props: Properties = new Properties()
       props.load(input)
       input.close()
       props.getProperty(key)
  }
}