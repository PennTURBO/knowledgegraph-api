package edu.upenn.turbo

import org.scalatra._
/*import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json._
import org.json4s._
import org.json4s.JsonDSL._*/

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

case class MedLookupResult(medFullName: String, mappedTerm: String)
case class MedFullName(fullName: List[String])
case class OntologyTerm(ontologyTerm: List[String])
case class OntologyLookupResult(ontologyTerm: String, meds: String)

class DashboardServlet extends ScalatraServlet with JacksonJsonSupport 
{
  protected implicit val jsonFormats: Formats = DefaultFormats
  before()
  {
      contentType = formats("json")
  }
  
  post("/medications/findOrderNamesFromInputString")
  {
      var cxn: RepositoryConnection = null
      var repository: Repository = null
      var repoManager: RemoteRepositoryManager = null
      var neo4jgraph: Neo4jGraph = null
      try
      {
          repoManager = new RemoteRepositoryManager(getFromProperties("serviceURL"))
          repoManager.setUsernameAndPassword(getFromProperties("username"), getFromProperties("password"))
          repoManager.initialize()
          repository = repoManager.getRepository(getFromProperties("repository"))
          cxn = repository.getConnection()
          
          neo4jgraph = Neo4jGraph.open("simple_graph")
          val g: GraphTraversalSource = neo4jgraph.traversal()
    
          val neo4j: Neo4jConnector = new Neo4jConnector
          neo4j.getOrderNames("node1", cxn, g)
      }
      finally
      {
          cxn.close()
          repository.shutDown()
          repoManager.shutDown()
          neo4jgraph.close()
      }
  }
  
  post("/medications/ontologyTermLookup")
  {
      val classesToLookup: String = request.body
      var extractedResult: Option[OntologyTerm] = None : Option[OntologyTerm]
      try
      {
        val parsedResult = parse(classesToLookup)
        extractedResult = Some(parsedResult.extract[OntologyTerm])
          
        var rxNormString: String = extractedResult.get.ontologyTerm.mkString("<","><",">")
      
        var cxn: RepositoryConnection = null
        var repository: Repository = null
        var repoManager: RemoteRepositoryManager = null
        try
        {
            repoManager = new RemoteRepositoryManager(getFromProperties("serviceURL"))
            repoManager.setUsernameAndPassword(getFromProperties("username"), getFromProperties("password"))
            repoManager.initialize()
            repository = repoManager.getRepository(getFromProperties("repository"))
            cxn = repository.getConnection()
  
            val tupleQuery: TupleQuery = cxn.prepareTupleQuery(QueryLanguage.SPARQL, getOntologyTermLookupQuery(rxNormString))
            val result: TupleQueryResult = tupleQuery.evaluate()
            var listToReturn: ArrayBuffer[OntologyLookupResult] = ArrayBuffer[OntologyLookupResult]()
            
            while (result.hasNext)
            {
                 val bindingSet: BindingSet = result.next()
                 val rxNormString: String = bindingSet.getBinding("rxNormTerm").toString
                 val fullNameString: String = bindingSet.getBinding("fullName").toString
                 listToReturn += 
                   OntologyLookupResult(rxNormString.substring(11), 
                       fullNameString.substring(10, fullNameString.length-2))
            }
            listToReturn
        }
        finally
        {
            cxn.close()
            repository.shutDown()
            repoManager.shutDown()
        }
      }
      catch
      {
          case e: JsonParseException => BadRequest(Map("message" -> "Unable to parse JSON"))
      }
  }
  
  post("/medications/orderNameLookup")
  {
        val medsToLookup: String = request.body
        var extractedResult: Option[MedFullName] = None : Option[MedFullName]
        try
        {
          val parsedResult = parse(medsToLookup)
          extractedResult = Some(parsedResult.extract[MedFullName])
          var fullNameString: String = extractedResult.get.fullName.mkString("\"","\"\"","\"")
          var cxn: RepositoryConnection = null
          var repository: Repository = null
          var repoManager: RemoteRepositoryManager = null
          try
          {
              repoManager = new RemoteRepositoryManager(getFromProperties("serviceURL"))
              repoManager.setUsernameAndPassword(getFromProperties("username"), getFromProperties("password"))
              repoManager.initialize()
              repository = repoManager.getRepository(getFromProperties("repository"))
              cxn = repository.getConnection()
  
              val tupleQuery: TupleQuery = cxn.prepareTupleQuery(QueryLanguage.SPARQL, getOrderNameLookupQuery(fullNameString))
              val result: TupleQueryResult = tupleQuery.evaluate()
              var listToReturn: ArrayBuffer[MedLookupResult] = ArrayBuffer[MedLookupResult]()
              
              while (result.hasNext)
              {
                   val bindingSet: BindingSet = result.next()
                   val fullNameStr: String = bindingSet.getBinding("fullName").toString
                   val medMappedStr: String = bindingSet.getBinding("rxnMapping").toString
                   listToReturn += 
                     MedLookupResult(fullNameStr.substring(10, fullNameStr.size-1), 
                         medMappedStr.substring(11, medMappedStr.size-2))
              }
              listToReturn
          }
          finally
          {
              cxn.close()
              repository.shutDown()
              repoManager.shutDown()
          }
        }
        catch
        {
            case e: JsonParseException => BadRequest(Map("message" -> "Unable to parse JSON"))
        }
  }
  
  def getFromProperties(key: String, file: String = "dashboard.properties"): String =
  {
       val input: FileInputStream = new FileInputStream(file)
       val props: Properties = new Properties()
       props.load(input)
       input.close()
       props.getProperty(key)
  }
  
  def getOntologyTermLookupQuery(rxNormString: String): String =
  {
    """
  	PREFIX mydata: <http://example.com/resource/>
    PREFIX graphBuilder: <http://graphBuilder.org/>
    PREFIX turbo: <http://transformunify.org/ontologies/>
    PREFIX obo: <http://purl.obolibrary.org/obo/>
    PREFIX pmbb: <http://www.itmat.upenn.edu/biobank/>
    PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
    PREFIX ns1: <http://www.geneontology.org/formats/oboInOwl#>
    PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
    select 
    distinct ?fullName ?rxNormTerm
    where
    {
        Values ?rxNormTerm {"""+rxNormString+"""}
        {
            graph mydata:wes_pds__med_standard.csv 
            {
                ?standard a mydata:medStandard ;
                          mydata:PK_MEDICATION_ID ?fkmi ;
                          mydata:FULL_NAME ?fullName .
                    ?standard  mydata:RXNORM ?rxNormNumber .
            }
            BIND(STRAFTER(str(?rxNormTerm), "http://purl.bioontology.org/ontology/RXNORM/") AS ?rxNormNumber2)
    		FILTER(?rxNormNumber = ?rxNormNumber2)
        }
        UNION
        {
            graph mydata:wes_pds__med_standard.csv 
            {
                ?standard a mydata:medStandard ;
                          mydata:PK_MEDICATION_ID ?fkmi ;
                          mydata:FULL_NAME ?fullName .
            }
            graph mydata:med_standard_FULL_NAME_query_expansion 
            {
                ?extension rdf:type mydata:Row ;
                           mydata:PK_MEDICATION_ID ?fkmi ;
                           mydata:expanded.query ?expandedName .
            }
            graph mydata:med_standard_FULL_NAME_bioportal_search 
            {
                ?searchRes rdf:type mydata:Row ;
                           mydata:order ?expandedName ;
                           mydata:rank "1" .
            } 
            graph mydata:RxnIfAvailable 
            {
                ?searchRes mydata:RxnIfAvailable ?rxNormTerm ;
                           mydata:RxnAvailable "true"^^xsd:boolean .
            }   
        }
    }
  """
  }
  
  def getOrderNameLookupQuery(fullNameString: String): String =
  {
   """
  	PREFIX mydata: <http://example.com/resource/>
    PREFIX graphBuilder: <http://graphBuilder.org/>
    PREFIX turbo: <http://transformunify.org/ontologies/>
    PREFIX obo: <http://purl.obolibrary.org/obo/>
    PREFIX pmbb: <http://www.itmat.upenn.edu/biobank/>
    PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
    PREFIX ns1: <http://www.geneontology.org/formats/oboInOwl#>
    PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
    select 
    ?fullName ?expandedName ?rxnMapping
    where
    {
        Values ?fullName {"""+fullNameString+"""}
        graph mydata:wes_pds__med_standard.csv 
        {
            ?standard a mydata:medStandard ;
                      mydata:PK_MEDICATION_ID ?fkmi ;
                      mydata:FULL_NAME ?fullName .
            optional {
                ?standard  mydata:RXNORM ?rxnval .
            }
        }
        graph mydata:med_standard_FULL_NAME_query_expansion 
        {
            ?extension rdf:type mydata:Row ;
                       mydata:PK_MEDICATION_ID ?fkmi ;
                       mydata:expanded.query ?expandedName .
        }
        graph mydata:med_standard_FULL_NAME_bioportal_search 
        {
            ?searchRes rdf:type mydata:Row ;
                       mydata:order ?expandedName ;
            		   mydata:rank "1" .
        } 
        graph mydata:RxnIfAvailable 
        {
            ?searchRes mydata:RxnIfAvailable ?rxnFromBioportalMapping ;
                       mydata:RxnAvailable "true"^^xsd:boolean .
        }
        BIND(uri(concat("http://purl.bioontology.org/ontology/RXNORM/", ?rxnval)) AS ?rxnFromDataset)
        BIND(If(BOUND(?rxnFromDataset), ?rxnFromDataset, ?rxnFromBioportalMapping) AS ?rxnMapping)
    }
  """
  }
}