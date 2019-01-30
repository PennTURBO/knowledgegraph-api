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
import org.eclipse.rdf4j.repository.RepositoryConnection

import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json._
import com.fasterxml.jackson.core.JsonParseException
import java.util.Properties
import java.io.FileInputStream
import org.neo4j.graphdb._
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import com.fasterxml.jackson.databind.JsonMappingException

import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer
import java.io.File

case class FullNameResults(mappedInputTerm: String, resultsList: Array[String])
case class GraphUpdateTime(dateOfUpdate: String, timeOfUpdate: String)
case class FullNameInput(searchTerm: String)
case class LuceneMedResults(searchTerm: String, searchResults: Array[Map[String, String]])
case class LuceneDiagResults(searchTerm: String, searchResults: Array[Map[String, String]])
case class DrugClassInputs(searchList: Array[String])
case class DrugResults(resultsList: Map[String, Array[String]])
case class TwoDimensionalArrListResults(resultsList: Array[Array[String]])

class DashboardServlet extends ScalatraServlet with JacksonJsonSupport 
{
  val logger = LoggerFactory.getLogger("turboAPIlogger")
  val graphDB: GraphDBConnector = new GraphDBConnector
  val neo4j: Neo4jConnector = new Neo4jConnector
  protected implicit val jsonFormats: Formats = DefaultFormats
  before()
  {
      contentType = formats("json")
  }

  post("/diagnoses/getDiseaseURIsFromICDCodes")
  {
      logger.info("Received a post request")
      var cxn: RepositoryConnection = null
      var repository: Repository = null
      var repoManager: RemoteRepositoryManager = null
      var neo4jgraph: Neo4jGraph = null
      var parsedResult: Array[String] = null

      try 
      { 
          val userInput = request.body
          logger.info("received: " + userInput)
          val extractedResult = parse(userInput).extract[DrugClassInputs]
          parsedResult = extractedResult.searchList
          logger.info("extracted search term")

          try
          {
              repoManager = new RemoteRepositoryManager(getFromProperties("serviceURL"))
              repoManager.setUsernameAndPassword(getFromProperties("username"), getFromProperties("password"))
              repoManager.initialize()
              repository = repoManager.getRepository(getFromProperties("diagnoses_repository"))
              cxn = repository.getConnection()
              logger.info("Successfully connected to triplestore")
            
              TwoDimensionalArrListResults(graphDB.getDiseaseURIs(parsedResult, cxn))
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

  post("/medications/medicationTextSearch")
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
              repository = repoManager.getRepository(getFromProperties("medications_repository"))
              cxn = repository.getConnection()
              logger.info("Successfully connected to triplestore")
            
              val topResults = graphDB.getBestMatchTermForMedicationLookup(cxn, parsedResult, 10)
              if (topResults == None)
              {
                  val noContentMessage = "Your input of \"" + parsedResult + "\" returned no matches."
                  NoContent(Map("message" -> noContentMessage))
              }
              else
              {
                  var luceneResAsMaps = new ArrayBuffer[Map[String, String]]
                  for (a <- topResults.get)
                  {
                     var tempMap = Map("IRI" -> a(0), "label" -> a(1).replaceAll("@en", "").replaceAll("\"", ""))
                     luceneResAsMaps += tempMap
                  }
                  LuceneMedResults(parsedResult, luceneResAsMaps.toArray)
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

  post("/diagnoses/diagnosisTextSearch")
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
            
              val topResults = graphDB.getBestMatchTermForDiagnosisLookup(cxn, parsedResult, 10)
              if (topResults == None)
              {
                  val noContentMessage = "Your input of \"" + parsedResult + "\" returned no matches."
                  NoContent(Map("message" -> noContentMessage))
              }
              else
              {
                  var luceneResAsMaps = new ArrayBuffer[Map[String, String]]
                  for (a <- topResults.get)
                  {
                     var tempMap = Map("IRI" -> a(0), "label" -> a(1).replaceAll("@en", "").replaceAll("\"", ""))
                     luceneResAsMaps += tempMap
                  }
                  LuceneDiagResults(parsedResult, luceneResAsMaps.toArray)
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
                  neo4jgraph = Neo4jGraph.open("mondo.graph")
              } 
              catch 
              {
                  case e: Throwable => e.printStackTrace()
              }
              val g: GraphTraversalSource = neo4jgraph.traversal()
              logger.info("Successfully connected to property graph")
            
              DrugResults(neo4j.getHopsAwayFromTopLevelClass(parsedResult, "http://purl.obolibrary.org/obo/MONDO_0000001", g))
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