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

import scala.collection.mutable.ArrayBuffer

case class MedLookupResult(medFullName: String, mappedTerm: String)
case class MedFullName(fullName: List[String])

class drivetrainWebServlet extends ScalatraServlet with JacksonJsonSupport {
  protected implicit val jsonFormats: Formats = DefaultFormats
  before()
  {
      contentType = formats("json")
  }
  get("/") 
  {
  
  }
  
  post("/medications")
  {
        val medsToLookup: String = request.body
        val parsedResult = parse(medsToLookup)
        val extractedResult = parsedResult.extract[MedFullName]
        
        var fullNameString: String = extractedResult.fullName.mkString("\"","\"\"","\"")
        
        var cxn: RepositoryConnection = null
        var repository: Repository = null
        var repoManager: RemoteRepositoryManager = null
        try
        {
            repoManager = new RemoteRepositoryManager("http://turbo-prd-db01.pmacs.upenn.edu:7200/")
            repoManager.setUsernameAndPassword("hfree", "ObibIsTheBest")
            repoManager.initialize()
            repository = repoManager.getRepository("med_orders_ncbo_mappings")
            cxn = repository.getConnection()
            //println("string for query: " + fullNameString)

            val query = """
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
            val tupleQuery: TupleQuery = cxn.prepareTupleQuery(QueryLanguage.SPARQL, query)
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
}
