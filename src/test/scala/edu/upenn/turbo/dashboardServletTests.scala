package edu.upenn.turbo

import org.scalatest.BeforeAndAfterAll
import org.scalatra.test.scalatest._

import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph

class DashboardServletTests extends ScalatraFunSuite with BeforeAndAfterAll with DashboardProperties {

  addServlet(classOf[DashboardServlet], "/*")

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

      diagCxn.close()
      diagRepository.shutDown()
      diagRepoManager.shutDown()

      medCxn.close()
      medRepository.shutDown()
      medRepoManager.shutDown()

      ontCxn.close()
      ontRepository.shutDown()
      ontRepoManager.shutDown()
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
      val res = post("/diagnoses/getICDCodesFromDiseaseURI", "{\"searchTerm\":\"http://purl.obolibrary.org/obo/MONDO_0004992\"}")
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
      val res = post("/medications/findOrderNamesFromInputURI", "{\"searchList\":[\"http://purl.obolibrary.org/obo/CHEBI_6942\"]}")
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
  
  test("POST /medications/findOrderNamesFromInputURI bad params")
  {
      val res = post("/medications/findOrderNamesFromInputURI", "{bad_param}")
      {
        status should equal (400)
      }
  }
  
  //204 result expected because it uses outdated lucene index which does not exist in new repo
  test("POST /medications/medicationTextSearch with params")
  {
      val res = post("/medications/medicationTextSearch", "{\"searchTerm\":\"analgesic\"}")
      {
        status should equal (204)
      }
  }
  
  test("POST /medications/medicationTextSearch no params")
  {
      val res = post("/medications/medicationTextSearch")
      {
        status should equal (400)
      }
  }
  
  test("POST /medications/medicationTextSearch params with no results")
  {
      val res = post("/medications/medicationTextSearch", "{\"searchTerm\":\"not_a_med\"}")
      {
        status should equal (204)
      }
  }
  
  test("POST /medications/medicationTextSearch bad params")
  {
      val res = post("/medications/medicationTextSearch", "{bad_param}")
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
      val res = post("/diagnoses/getDiseaseURIsFromICDCodes", "{\"searchList\":[\"http://purl.bioontology.org/ontology/ICD9CM/285.9\", \"http://purl.bioontology.org/ontology/ICD10CM/E00\", \"http://purl.bioontology.org/ontology/ICD9CM/103.9\"]}")
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
