package edu.upenn.turbo

import org.scalatest.BeforeAndAfterAll
import org.scalatra.test.scalatest._

import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph

import org.eclipse.rdf4j.query.BindingSet
import org.eclipse.rdf4j.query.QueryLanguage
import scala.collection.mutable.HashMap

class DashboardServletTests extends ScalatraFunSuite with BeforeAndAfterAll with DashboardProperties {

  addServlet(classOf[DashboardServlet], "/*")
  var medMapCxn: RepositoryConnection = null

  override def beforeAll()
  {
      super.beforeAll()
      //establish connections to graph databases
      /*println("connecting to neo4j...")

      val neo4jgraph = Neo4jGraph.open("neo4j.graph")
      Neo4jGraphConnection.setGraph(neo4jgraph)*/

      println("connecting to graph db...")

      val diagRepoManager = new RemoteRepositoryManager(getFromProperties("serviceURL"))
      diagRepoManager.setUsernameAndPassword(getFromProperties("username"), getFromProperties("password"))
      diagRepoManager.initialize()
      val diagRepository = diagRepoManager.getRepository(getFromProperties("diagnoses_repository"))
      val diagCxn = diagRepository.getConnection()

      GraphDbConnection.setDiagRepoManager(diagRepoManager)
      GraphDbConnection.setDiagRepository(diagRepository)
      GraphDbConnection.setDiagConnection(diagCxn)

      val medRepoManager = new RemoteRepositoryManager(getFromProperties("serviceURL"))
      medRepoManager.setUsernameAndPassword(getFromProperties("username"), getFromProperties("password"))
      medRepoManager.initialize()
      val medRepository = medRepoManager.getRepository(getFromProperties("medications_repository"))
      val medCxn = medRepository.getConnection()

      GraphDbConnection.setMedRepoManager(medRepoManager)
      GraphDbConnection.setMedRepository(medRepository)
      GraphDbConnection.setMedConnection(medCxn)

      val ontRepoManager = new RemoteRepositoryManager(getFromProperties("serviceURL"))
      ontRepoManager.setUsernameAndPassword(getFromProperties("username"), getFromProperties("password"))
      ontRepoManager.initialize()
      val ontRepository = ontRepoManager.getRepository(getFromProperties("ontology_repository"))
      val ontCxn = ontRepository.getConnection()

      GraphDbConnection.setOntRepoManager(ontRepoManager)
      GraphDbConnection.setOntRepository(ontRepository)
      GraphDbConnection.setOntConnection(ontCxn)

      val medMapRepoManager = new RemoteRepositoryManager(getFromProperties("serviceURL"))
      medMapRepoManager.setUsernameAndPassword(getFromProperties("username"), getFromProperties("password"))
      medMapRepoManager.initialize()
      val medMapRepository = medMapRepoManager.getRepository(getFromProperties("medMap_ontology_repository"))
      medMapCxn = medMapRepository.getConnection()

      GraphDbConnection.setMedMapRepoManager(medMapRepoManager)
      GraphDbConnection.setMedMapRepository(medMapRepository)
      GraphDbConnection.setMedMapConnection(medMapCxn)
  }
  override def afterAll()
  {
      super.afterAll()
      
      /*val neo4jgraph = Neo4jGraphConnection.getGraph()
      neo4jgraph.close()*/

      val diagRepoManager = GraphDbConnection.getDiagRepoManager()
      val diagRepository = GraphDbConnection.getDiagRepository()
      val diagCxn = GraphDbConnection.getDiagConnection()

      val medRepoManager = GraphDbConnection.getMedRepoManager()
      val medRepository = GraphDbConnection.getMedRepository()
      val medCxn = GraphDbConnection.getMedConnection()

      val ontRepoManager = GraphDbConnection.getOntRepoManager()
      val ontRepository = GraphDbConnection.getOntRepository()
      val ontCxn = GraphDbConnection.getOntConnection()

      val medMapRepoManager = GraphDbConnection.getMedMapRepoManager()
      val medMapRepository = GraphDbConnection.getMedMapRepository()
      val medMapCxn = GraphDbConnection.getMedMapConnection()

      diagCxn.close()
      diagRepository.shutDown()
      diagRepoManager.shutDown()

      medCxn.close()
      medRepository.shutDown()
      medRepoManager.shutDown()

      ontCxn.close()
      ontRepository.shutDown()
      ontRepoManager.shutDown()

      medMapCxn.close()
      medMapRepository.shutDown()
      medMapRepoManager.shutDown()
  }

  test("GET / on dashboardServlet should return status 200") 
  {
    get("/") 
    {
      assert(status == 200)
    }
  }
  
  test("POST /diagnoses/notExists")
  {
    val res = post("/notExists")
    {
      status should equal (404)
    }
  }
  
  test("POST /diagnoses/getICDCodesFromDiseaseURI with params")
  {
      val res = post("/diagnoses/getICDCodesFromDiseaseURI", "{\"searchTerm\":\"http://purl.obolibrary.org/obo/MONDO_0004992\",\"filterMethod\":\"LEAF\"}")
      {
        status should equal (200)
      }
  }
  
  test("POST /diagnoses/getICDCodesFromDiseaseURI no params")
  {
      val res = post("/diagnoses/getICDCodesFromDiseaseURI")
      {
        status should equal (400)
      }
  }
  
  test("POST /diagnoses/getICDCodesFromDiseaseURI bad params")
  {
      val res = post("/diagnoses/getICDCodesFromDiseaseURI", "{bad_param}")
      {
        status should equal (400)
      }
  }
  
  test("POST /diagnoses/diagnosisTextSearch with params")
  {
      val res = post("/diagnoses/diagnosisTextSearch", "{\"searchTerm\":\"diabetes\"}")
      {
        status should equal (200)
      }
  }

  test("POST /diagnoses/diagnosisTextSearch no params")
  {
      val res = post("/diagnoses/diagnosisTextSearch")
      {
        status should equal (400)
      }
  }
  
  test("POST /diagnoses/diagnosisTextSearch bad params")
  {
      val res = post("/diagnoses/diagnosisTextSearch", "{bad_param}")
      {
        status should equal (400)
      }
  }
  
  test("POST /diagnoses/diagnosisTextSearch params with no results")
  {
      val res = post("/diagnoses/diagnosisTextSearch", "{\"searchTerm\":\"not_a_disease\"}")
      {
        status should equal (204)
      }
  }

  test("POST /diagnoses/getSemanticContextForDiseaseURIs with params")
  {
      val res = post("/diagnoses/getSemanticContextForDiseaseURIs", "{\"searchList\":[\"http://purl.obolibrary.org/obo/MONDO_0005002\",\"http://purl.obolibrary.org/obo/MONDO_0011751\"]}")
      {
        status should equal (200)
      }
  }
  
  test("POST /medications/findOrderNamesFromInputURI with params")
  {
      val res = post("/medications/findOrderNamesFromInputURI", "{\"searchList\":{\"active_ingredient\":[\"http://purl.obolibrary.org/obo/CHEBI_116735\"], \"curated_role\":[\"http://purl.obolibrary.org/obo/CHEBI_36047\"]}}")
      {
        status should equal (200)
      }
  }
  
  test("POST /medications/findOrderNamesFromInputURI no params")
  {
      val res = post("/medications/findOrderNamesFromInputURI")
      {
        status should equal (400)
      }
  }

  test("POST /medications/medicationTextSearch no params")
  {
      val res = post("/medications/medicationTextSearch")
      {
        status should equal (400)
      }
  }

  test("POST /medications/medicationTextSearch with params")
  {
      val res = post("/medications/medicationTextSearch", "{\"searchTerm\":\"analgesic\"}")
      {
        status should equal (200)
      }
  }

  test("POST /medications/medicationTextSearch bad params")
  {
      val res = post("/medications/medicationTextSearch", "{\"searchTerm\":\"not_a_med\"}")
      {
        status should equal (204)
      }
  }

  test("medication text search QC from TMM Ontology")
  {
      val expectedResultsQuery = """
        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
        select 
        ?solrQueryExample (group_CONCAT(distinct ?solrKeyword;
                SEPARATOR="|") as ?keywords) (group_CONCAT(distinct str( ?expectedSolrResult) ;
                SEPARATOR="|") as ?expected)
        where {
            ?solrQueryExample a <http://transformunify.org/ontologies/TURBO_0022059> ;
                              rdfs:seeAlso ?applicable_emp ;
                              rdfs:label ?qlab ;
                              <http://transformunify.org/ontologies/TURBO_0022062> ?solrKeyword ;
                              <http://transformunify.org/ontologies/TURBO_0022061> ?expectedSolrResult .
            ?applicable_emp a ?t .
            optional {
                ?applicable_emp rdfs:label ?apemplab .
            }
            ?t rdfs:subClassOf <http://transformunify.org/ontologies/TURBO_0022023> .
        }
        group by ?solrQueryExample
      """
      val tupleQueryResult = medMapCxn.prepareTupleQuery(QueryLanguage.SPARQL, expectedResultsQuery).evaluate()
      var resultMap = new HashMap[String, String]
      while (tupleQueryResult.hasNext()) 
      {
          val bindingset: BindingSet = tupleQueryResult.next()
          var keywords = bindingset.getValue("keywords").toString
          var expectedResults = bindingset.getValue("expected").toString
          assert (!resultMap.contains(keywords), s"Duplicate keyword found in query result: $keywords")
          resultMap += keywords -> expectedResults
      }
      for ((keywords, expectedResults) <- resultMap)
      {
          var keywordList = keywords.split("\\|")
          var expectedResultsList = expectedResults.split("\\|")
          for (keyword <- keywordList)
          {
              var keywordNoQuotes = keyword
              if (keywordNoQuotes.charAt(0) == '"') keywordNoQuotes = keywordNoQuotes.substring(1)
              if (keywordNoQuotes.charAt(keywordNoQuotes.length-1) == '"') keywordNoQuotes = keywordNoQuotes.substring(0, keywordNoQuotes.length-1)
              println("searching term: " + keywordNoQuotes)
              val res = post("/medications/medicationTextSearch", "{\"searchTerm\":\""+keywordNoQuotes+"\"}")
              {
                 status should equal (200)
                 for (expectedResult <- expectedResultsList)
                 {
                    var erNoQuotes = expectedResult
                    if (erNoQuotes.charAt(0) == '"') erNoQuotes = erNoQuotes.substring(1)
                    if (erNoQuotes.charAt(erNoQuotes.length-1) == '"') erNoQuotes = erNoQuotes.substring(0, erNoQuotes.length-1)
                    println("expecting: " + erNoQuotes)
                    body should include (erNoQuotes)
                 }
              }
          }
      }
  }
  
  test("POST /medications/findOrderNamesFromInputURI bad params")
  {
      val res = post("/medications/findOrderNamesFromInputURI", "{bad_param}")
      {
        status should equal (400)
      }
  }

  test("POST /diagnoses/getDiseaseURIsFromICDCodes bad params")
  {
      val res = post("/diagnoses/getDiseaseURIsFromICDCodes", "{bad_param}")
      {
        status should equal (400)
      }
  }

  test("POST /diagnoses/getDiseaseURIsFromICDCodes with params")
  {
      val res = post("/diagnoses/getDiseaseURIsFromICDCodes", "{\"searchList\":[\"http://purl.bioontology.org/ontology/ICD9CM/285.9\", \"http://purl.bioontology.org/ontology/ICD10CM/E00\", \"http://purl.bioontology.org/ontology/ICD9CM/103.9\"],\"filterMethod\":\"LEAF\"}")
      {
        status should equal (200)
      }
  }
  
  test("POST /diagnoses/getDiseaseURIsFromICDCodes no params")
  {
      val res = post("/diagnoses/getDiseaseURIsFromICDCodes")
      {
        status should equal (400)
      }
  }

  test("POST /ontologies/getOmopConceptMap")
  {
      val res = post("/ontologies/getOmopConceptMap")
      {
        status should equal (200)
      }
  }
}
