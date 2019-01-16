package edu.upenn.turbo

import org.scalatra._
/*import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json._
import org.json4s._
import org.json4s.JsonDSL._*/
import org.json4s.MappingException
import java.nio.file.NoSuchFileException

import java.nio.file.{Files, Paths}
import java.nio.file.attribute.BasicFileAttributes

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
import com.fasterxml.jackson.databind.JsonMappingException

import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer

case class FullNameResults(mappedInputTerm: String, resultsList: Array[String])
case class GraphUpdateTime(dateOfUpdate: String, timeOfUpdate: String)
case class FullNameInput(searchTerm: String)
case class LuceneMedResults(searchTerm: String, luceneResults: Array[String])
case class LuceneDiagResults(searchTerm: String, luceneResults: Array[String])
case class DrugClassInputs(searchList: Array[String])
case class DrugResults(resultsList: Map[String, Array[String]])

class DashboardServlet extends ScalatraServlet with JacksonJsonSupport 
{
  val logger = LoggerFactory.getLogger(getClass)
  protected implicit val jsonFormats: Formats = DefaultFormats
  before()
  {
      contentType = formats("json")
  }

  post("/diagnoses/getICDCodesFromDiseaseURI")
  {
      logger.info("Received a post request")
      var cxn: RepositoryConnection = null
      var repository: Repository = null
      var repoManager: RemoteRepositoryManager = null
      var neo4jgraph: Neo4jGraph = null
      var parsedResult: String = null

      try 
      { 
          val userInput = request.body
          logger.info("received: " + userInput)
          val extractedResult = parse(userInput).extract[FullNameInput]
          parsedResult = extractedResult.searchTerm
          logger.info("extracted search term")

          try
          {
              repoManager = new RemoteRepositoryManager(getFromProperties("serviceURL"))
              repoManager.setUsernameAndPassword(getFromProperties("username"), getFromProperties("password"))
              repoManager.initialize()
              repository = repoManager.getRepository(getFromProperties("diagnoses_repository"))
              cxn = repository.getConnection()
              logger.info("Successfully connected to triplestore")
            
              val graphDB: GraphDBConnector = new GraphDBConnector
              FullNameResults(parsedResult, graphDB.getDiagnosisCodes(parsedResult, cxn))
          }
          catch
          {
              case e: RuntimeException => NoContent(Map("message" -> "There was a problem retrieving results from the triplestore."))
          }
          finally
          {
              cxn.close()
              repository.shutDown()
              repoManager.shutDown()
              logger.info("Connections closed.")
              println()
          }
      } 
      catch 
      {
          case e1: JsonParseException => BadRequest(Map("message" -> "Unable to parse JSON"))
          case e2: MappingException => BadRequest(Map("message" -> "Unable to parse JSON"))
          case e3: JsonMappingException => BadRequest(Map("message" -> "Did not receive any content in the request body"))
      }
  }

  post("/medications/findOrderNamesFromInputURI")
  {
      logger.info("Received a post request")
      var neo4jgraph: Neo4jGraph = null
      var parsedResult: String = null

      try 
      { 
          val userInput = request.body
          val extractedResult = parse(userInput).extract[FullNameInput]
          parsedResult = extractedResult.searchTerm
          logger.info("Input class: " + parsedResult)
          try 
          { 
              try 
              { 
                  neo4jgraph = Neo4jGraph.open("neo4j.graph")
              } 
              catch 
              {
                  case e: Throwable => e.printStackTrace()
              }
              val g: GraphTraversalSource = neo4jgraph.traversal()
              val neo4j: Neo4jConnector = new Neo4jConnector
              logger.info("Successfully connected to property graph")
              FullNameResults(parsedResult, neo4j.getOrderNames(parsedResult, g))
          }
          finally
          {
              neo4jgraph.close()
              logger.info("Connections closed.")
              println()
          }
      } 
      catch 
      {
          case e1: JsonParseException => BadRequest(Map("message" -> "Unable to parse JSON"))
          case e2: MappingException => BadRequest(Map("message" -> "Unable to parse JSON"))
          case e3: JsonMappingException => BadRequest(Map("message" -> "Did not receive any content in the request body"))
      }
  }

  post("/medications/luceneMedLookup")
  {
      var cxn: RepositoryConnection = null
      var repository: Repository = null
      var repoManager: RemoteRepositoryManager = null
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
              repository = repoManager.getRepository(getFromProperties("medications_repository"))
              cxn = repository.getConnection()
              logger.info("Successfully connected to triplestore")
            
              val topResults = getBestMatchTermForMedicationLookup(cxn, parsedResult, 10)
              if (topResults == None)
              {
                  val noContentMessage = "Your input of \"" + parsedResult + "\" returned no matches."
                  NoContent(Map("message" -> noContentMessage))
              }
              else
              {
                  var luceneResAsStrings = new ArrayBuffer[String]
                  for (a <- topResults.get)
                  {
                     val strToAdd = "IRI: " + a(0) + ", label: " + a(1).replaceAll("@en", "").replaceAll("\"", "")
                     luceneResAsStrings += strToAdd
                  }
                  LuceneMedResults(parsedResult, luceneResAsStrings.toArray)
              }
          }
          finally
          {
              cxn.close()
              repository.shutDown()
              repoManager.shutDown()
              logger.info("Connections closed.")
              println()
          }
      }
      catch 
      {
          case e1: JsonParseException => BadRequest(Map("message" -> "Unable to parse JSON"))
          case e2: MappingException => BadRequest(Map("message" -> "Unable to parse JSON"))
          case e3: JsonMappingException => BadRequest(Map("message" -> "Did not receive any content in the request body"))
      }
  }

  post("/diagnoses/luceneDiagLookup")
  {
      logger.info("Received a post request")
      var cxn: RepositoryConnection = null
      var repository: Repository = null
      var repoManager: RemoteRepositoryManager = null
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
              repository = repoManager.getRepository(getFromProperties("diagnoses_repository"))
              cxn = repository.getConnection()
              logger.info("Successfully connected to triplestore")
            
              val topResults = getBestMatchTermForDiagnosisLookup(cxn, parsedResult, 10)
              if (topResults == None)
              {
                  val noContentMessage = "Your input of \"" + parsedResult + "\" returned no matches."
                  NoContent(Map("message" -> noContentMessage))
              }
              else
              {
                  var luceneResAsStrings = new ArrayBuffer[String]
                  for (a <- topResults.get)
                  {
                     val strToAdd = "IRI: " + a(0) + ", label: " + a(1).replaceAll("@en", "").replaceAll("\"", "")
                     luceneResAsStrings += strToAdd
                  }
                  LuceneDiagResults(parsedResult, luceneResAsStrings.toArray)
              }
          }
          finally
          {
              cxn.close()
              repository.shutDown()
              repoManager.shutDown()
              logger.info("Connections closed.")
              println()
          }
      }
      catch 
      {
          case e1: JsonParseException => BadRequest(Map("message" -> "Unable to parse JSON"))
          case e2: MappingException => BadRequest(Map("message" -> "Unable to parse JSON"))
          case e3: JsonMappingException => BadRequest(Map("message" -> "Did not receive any content in the request body"))
      }
  }
  
  def getBestMatchTermForMedicationLookup(cxn: RepositoryConnection, userInput: String, limit: Integer = 1): Option[ArrayBuffer[ArrayBuffer[String]]] =
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
          limit 
      """ + limit

      val tupleQueryResult = cxn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()
      var buffResults = new ArrayBuffer[ArrayBuffer[String]]
      while (tupleQueryResult.hasNext()) 
      {
          val nextResult = tupleQueryResult.next
          val singleResult = ArrayBuffer(nextResult.getValue("entity").toString, nextResult.getValue("label").toString)
          buffResults += singleResult
      }
      if (buffResults.size != 0) Some(buffResults)
      else None
  }

  def getBestMatchTermForDiagnosisLookup(cxn: RepositoryConnection, userInput: String, limit: Integer = 1): Option[ArrayBuffer[ArrayBuffer[String]]] =
  {
      val query = """
          PREFIX : <http://www.ontotext.com/connectors/lucene#>
          PREFIX inst: <http://www.ontotext.com/connectors/lucene/instance#>
          PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
          PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
          SELECT ?entity ?score ?label {
              ?search a inst:MONDO_labelsAndSynonyms ;
                      :query "mondoLabel:"""+userInput+""" OR mondoExactSynonym:"""+userInput+"""" ;
                                                 :entities ?entity .
              ?entity :score ?score .
              ?entity rdfs:label ?label .
          }
          order by desc(?score)
          limit """ + limit

      val tupleQueryResult = cxn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()
      var buffResults = new ArrayBuffer[ArrayBuffer[String]]
      while (tupleQueryResult.hasNext()) 
      {
          val nextResult = tupleQueryResult.next
          val singleResult = ArrayBuffer(nextResult.getValue("entity").toString, nextResult.getValue("label").toString)
          buffResults += singleResult
      }
      if (buffResults.size != 0) Some(buffResults)
      else None
  }

  get("/medications/lastGraphUpdate")
  {
      try 
      { 
          logger.info("Checking date of last graph update")
          val pathStr = "neo4j.graph"
          val timeRes = Files.readAttributes(Paths.get(pathStr), classOf[BasicFileAttributes]).creationTime.toString.split("T")
          logger.info(timeRes(0) + " " + timeRes(1))
          val date = timeRes(0)
          logger.info("date of update: " + date)
          val time = timeRes(1).split("\\.")(0)
          logger.info("time of update: " + time)
          println()
          GraphUpdateTime(date, time)
      } 
      catch 
      {
          case e1: NoSuchFileException => InternalServerError(Map("message" -> "Graph file not found"))
          case e2: Throwable => InternalServerError(Map("message" -> "Unknown server error occurred"))
      }
      
  }

  post("/medications/findHopsAwayFromDrug")
  {
      logger.info("Received a post request")
      var neo4jgraph: Neo4jGraph = null
      var parsedResult: Array[String] = null

      try 
      { 
          val userInput = request.body
          val extractedResult = parse(userInput).extract[DrugClassInputs]
          parsedResult = extractedResult.searchList
          logger.info("Input class: " + parsedResult)
          try 
          { 
              try 
              { 
                  neo4jgraph = Neo4jGraph.open("neo4j.graph")
              } 
              catch 
              {
                  case e: Throwable => e.printStackTrace()
              }
              val g: GraphTraversalSource = neo4jgraph.traversal()
              logger.info("Successfully connected to property graph")
            
              val neo4j: Neo4jConnector = new Neo4jConnector
              DrugResults(neo4j.getHopsAwayFromDrug(parsedResult, g))
          }
          finally
          {
              neo4jgraph.close()
              logger.info("Connections closed.")
              println()
          }
      } 
      catch 
      {
          case e1: JsonParseException => BadRequest(Map("message" -> "Unable to parse JSON"))
          case e2: MappingException => BadRequest(Map("message" -> "Unable to parse JSON"))
          case e3: JsonMappingException => BadRequest(Map("message" -> "Did not receive any content in the request body"))
      }
  }
  
  def getFromProperties(key: String, file: String = "turboAPI.properties"): String =
  {
       val input: FileInputStream = new FileInputStream(file)
       val props: Properties = new Properties()
       props.load(input)
       input.close()
       props.getProperty(key)
  }
}