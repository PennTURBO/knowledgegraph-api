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
import com.fasterxml.jackson.databind.JsonMappingException

import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import java.io.File
import java.util.ArrayList

import org.apache.solr.client.solrj._
import org.apache.solr.client.solrj.impl.HttpSolrClient

import org.neo4j.driver.exceptions.ServiceUnavailableException

case class GraphUpdateTime(dateOfUpdate: String, timeOfUpdate: String)
case class MedFullNameInput(searchList: Map[String, Array[String]])
case class MedFullNameResults(resultsMap: HashMap[String, ArrayBuffer[String]])
case class MedicationFreeText(searchTerm: String)
case class DiagnosisFreeText(searchTerm: String)
case class DiagnosisTermInput(searchTerm: String, filterMethod: String)
case class OmopConceptIdInput(searchTerm: String)
case class OmopConceptIdUri(result: String)
case class OmopConceptMap(result: Map[String, String])
case class SolrMedResults(searchTerm: String, searchResults: List[Map[String, String]])
case class LuceneDiagResults(searchTerm: String, searchResults: Array[Map[String, String]])
case class DrugClassInputs(searchList: Array[String], filterMethod: String)
case class SemanticContextDrugInput(searchList: Array[String])
case class DiagnosisPathways(resultsList: Array[String])
case class DrugHopsResults(resultsList: Map[String, Array[String]])
case class DiagnosisCodeResult(searchTerm: String, resultsList: HashMap[String, ArrayBuffer[String]])
case class TwoDimensionalArrListResults(resultsList: Array[Array[String]])
case class ListOfStringToStringHashMapsResult(resultsList: Array[HashMap[String,String]])
case class GraphMlResult(data: String)

class DashboardServlet extends ScalatraServlet with JacksonJsonSupport with DashboardProperties
{
  val graphDB: GraphDBConnector = new GraphDBConnector

  val diagCxn = GraphDbConnection.getDiagConnection()
  val medCxn = GraphDbConnection.getMedConnection()
  val ontCxn = GraphDbConnection.getOntConnection()
  val medMapCxn = GraphDbConnection.getMedMapConnection()
  val neo4jCypherService = Neo4jCypherServiceHolder.getService()

  val logger = LoggerFactory.getLogger("turboAPIlogger")

  protected implicit val jsonFormats: Formats = DefaultFormats
  before()
  {
      contentType = formats("json")
  }

  post("/diagnoses/getDiseaseURIsFromICDCodes")
  {
      logger.info("Received a post request")
      var parsedResult: Array[String] = null
      var filterMethod: String = null
      try 
      { 
          val userInput = request.body
          logger.info("received: " + userInput)
          val extractedResult = parse(userInput).extract[DrugClassInputs]
          parsedResult = extractedResult.searchList
          filterMethod = extractedResult.filterMethod
          logger.info("extracted search term")

          if (parsedResult.size == 0) BadRequest(Map("message" -> "Unable to parse JSON"))
          else
          {
              try
              {
                  val res = graphDB.getDiseaseURIs(parsedResult, filterMethod, diagCxn)
                  if (res.size == 0)
                  {
                      val noContentMessage = "Your input of \"" + parsedResult + "\" returned no matches."
                      NoContent(Map("message" -> noContentMessage))
                  }
                  else ListOfStringToStringHashMapsResult(res)
              }
              catch
              {
                  case e: RuntimeException => 
                  {
                      println(e.toString)
                      InternalServerError(Map("message" -> "There was a problem retrieving results from the triplestore."))
                  }
              }
          }
      } 
      catch 
      {
          case e1: JsonParseException => BadRequest(Map("message" -> "Unable to parse JSON"))
          case e2: MappingException => BadRequest(Map("message" -> "Unable to parse JSON"))
          case e3: JsonMappingException => BadRequest(Map("message" -> "Did not receive any content in the request body"))
      }
  }

  post("/diagnoses/getSemanticContextForDiseaseURIs")
  {
      logger.info("Received a post request")
      var parsedResult: Array[String] = null
      try 
      { 
          val userInput = request.body
          logger.info("received: " + userInput)
          val extractedResult = parse(userInput).extract[SemanticContextDrugInput]
          parsedResult = extractedResult.searchList
          logger.info("extracted search term")
          if (parsedResult.size == 0) BadRequest(Map("message" -> "Unable to parse JSON"))
          else
          {
              try
              {
                  val res = graphDB.getSemanticContextForDiseaseURIs(parsedResult, diagCxn)
                  if (res.size == 0)
                  {
                      val noContentMessage = "Your input of \"" + parsedResult + "\" returned no matches."
                      NoContent(Map("message" -> noContentMessage))
                  }
                  else TwoDimensionalArrListResults(res)
              }
              catch
              {
                  case e: RuntimeException => InternalServerError(Map("message" -> "There was a problem retrieving results from the triplestore."))
              }
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
      var parsedResult: String = null
      var filterMethod: String = null
      try 
      { 
          val userInput = request.body
          logger.info("received: " + userInput)
          val extractedResult = parse(userInput).extract[DiagnosisTermInput]
          parsedResult = extractedResult.searchTerm
          filterMethod = extractedResult.filterMethod
          logger.info("extracted search term")

          try
          { 
              val res = graphDB.getDiagnosisCodes(parsedResult, filterMethod, diagCxn)
              if (res.size == 0)
              {
                  val noContentMessage = "Your input of \"" + parsedResult + "\" returned no matches."
                  NoContent(Map("message" -> noContentMessage))
              }
              else ListOfStringToStringHashMapsResult(res)
          }
          catch
          {
              case e: RuntimeException => 
              {
                println(e.toString)
                InternalServerError(Map("message" -> "There was a problem retrieving results from the triplestore."))
              }
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
      var parsedResult: Map[String, Array[String]] = null
      try 
      {
          val userInput = request.body
          val extractedResult = parse(userInput).extract[MedFullNameInput]
          parsedResult = extractedResult.searchList
          if (parsedResult.size == 0) BadRequest(Map("message" -> "Unable to parse JSON"))
          else
          {
            val res = graphDB.getMedicationFullNameResults(parsedResult, medCxn, medMapCxn)
            if (res.size == 0)
            {
                val noContentMessage = "Your input of \"" + parsedResult + "\" returned no matches."
                NoContent(Map("message" -> noContentMessage))
            }
            else MedFullNameResults(res)
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
      var parsedResult: String = null
      try
      {
          val userInput = request.body
          val extractedResult = parse(userInput).extract[MedicationFreeText]
          parsedResult = extractedResult.searchTerm
          logger.info("search term: " + parsedResult)

          val connectionString = getFromProperties("solrURL")+"solr/"+getFromProperties("solrCollection")
          println(s"connecting to $connectionString")
          val solrClient = new HttpSolrClient.Builder(connectionString).build()
          val solrQuery = new SolrQuery()
          solrQuery.set("q", parsedResult)
          solrQuery.set("defType", "edismax")
          solrQuery.set("qf", "medlabel tokens")
          solrQuery.set("fl", "id medlabel score employment")
          val response = solrClient.query(solrQuery)
          val results = response.getResults()
          println("results size: " + results.getNumFound())

          val resList: ArrayBuffer[Map[String, String]] = new ArrayBuffer[Map[String, String]]()
          for (i <- 0 to results.size()-1)
          {
              println("result: " + results.get(i))
              val thisMap = new HashMap[String, String]
              thisMap += "medlabel" -> results.get(i).getFieldValue("medlabel").asInstanceOf[ArrayList[String]].get(0)
              thisMap += "id" -> results.get(i).getFieldValue("id").asInstanceOf[String]
              thisMap += "employment" -> results.get(i).getFieldValue("employment").asInstanceOf[ArrayList[String]].get(0)
              thisMap += "score" -> results.get(i).getFieldValue("score").asInstanceOf[Float].toString
              resList += thisMap.toMap
          }
          if (results.getNumFound() == 0)
          {
              val noContentMessage = "Your input of \"" + parsedResult + "\" returned no matches."
              NoContent(Map("message" -> noContentMessage))
          }
          else SolrMedResults(parsedResult, resList.toList)
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
      var parsedResult: String = null
      try 
      { 
          val userInput = request.body
          val extractedResult = parse(userInput).extract[DiagnosisFreeText]
          parsedResult = extractedResult.searchTerm
          logger.info("search term: " + parsedResult)
          try
          {
              val topResults = graphDB.getBestMatchTermForDiagnosisLookup(diagCxn, parsedResult, 10)
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
          catch
          {
              case e: RuntimeException => InternalServerError(Map("message" -> "There was a problem retrieving results from the triplestore."))
          }
      }
      catch 
      {
          case e1: JsonParseException => BadRequest(Map("message" -> "Unable to parse JSON"))
          case e2: MappingException => BadRequest(Map("message" -> "Unable to parse JSON"))
          case e3: JsonMappingException => BadRequest(Map("message" -> "Did not receive any content in the request body"))
      }
  }

  post("/diagnoses/getAllDiagnosisMappingPaths")
  {
      logger.info("Received a post request")

      try
      { 
          DiagnosisPathways(graphDB.getDiagnosisMappingPathways(diagCxn))
      }
      catch
      {
          case e: RuntimeException => InternalServerError(Map("message" -> "There was a problem retrieving results from the triplestore."))
      }
  }

  post("/diagnoses/getGraphMlContextForDiseaseURI")
  {
      logger.info("Recieved POST to /diagnoses/getGraphMlContextForDiseaseURI")

      var parsedResult: String = null
      try {
          val userInput = request.body
          val extractedResult = parse(userInput).extract[DiagnosisFreeText]
          parsedResult = extractedResult.searchTerm
          logger.info("search term: " + parsedResult)

          neo4jCypherService.getDiseaseContextGraphMl(parsedResult) match {
            case Some(i) => Ok(i)
            case None => Ok("")
          }
      }
      catch {
          case e1: IllegalArgumentException => BadRequest(Map("message" -> e1.getMessage))
          case e2: ServiceUnavailableException => ServiceUnavailable(Map("message" -> e2.getMessage))
          case e3: Exception =>
            {
              logger.error("uncaught exception")
              logger.error(e3.toString)
              throw e3
            }
      }
  }

  post("/ontologies/getOmopConceptMap")
  {
      logger.info("Received a post request")
      try 
      { 

          val res: Map[String, String] = graphDB.getOmopConceptMap(ontCxn)
          OmopConceptMap(res)
      } 
      catch 
      {
          case e1: JsonParseException => BadRequest(Map("message" -> "Unable to parse JSON"))
          case e2: MappingException => BadRequest(Map("message" -> "Unable to parse JSON"))
          case e3: JsonMappingException => BadRequest(Map("message" -> "Did not receive any content in the request body"))
          case e4: NumberFormatException => BadRequest(Map("message" -> "The input receieved was not a valid integer"))
          case e5: RuntimeException => NoContent(Map("message" -> "Unknown internal server error"))
      }
  }
}