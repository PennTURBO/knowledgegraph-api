package edu.upenn.turbo

import org.scalatra.test.scalatest._

class dashboardServletTests extends ScalatraFunSuite {

  addServlet(classOf[DashboardServlet], "/*")

  test("GET / on dashboardServlet should return status 200") 
  {
    get("/") 
    {
      status should equal (200)
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
  
  test("POST /diagnoses/luceneDiagLookup with params")
  {
      val res = post("/diagnoses/luceneDiagLookup", "{\"searchTerm\":\"diabetes\"}")
      {
        status should equal (200)
      }
  }

  test("POST /diagnoses/luceneDiagLookup no params")
  {
      val res = post("/diagnoses/luceneDiagLookup")
      {
        status should equal (400)
      }
  }
  
  test("POST /diagnoses/luceneDiagLookup bad params")
  {
      val res = post("/diagnoses/luceneDiagLookup", "{bad_param}")
      {
        status should equal (400)
      }
  }
  
  test("POST /diagnoses/luceneDiagLookup params with no results")
  {
      val res = post("/diagnoses/luceneDiagLookup", "{\"searchTerm\":\"not_a_disease\"}")
      {
        status should equal (204)
      }
  }
  
  test("POST /medications/findOrderNamesFromInputURI with params")
  {
      val res = post("/medications/findOrderNamesFromInputURI", "{\"searchTerm\":\"http://purl.obolibrary.org/obo/CHEBI_35480\"}")
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
  
  test("POST /medications/luceneMedLookup with params")
  {
      val res = post("/medications/luceneMedLookup", "{\"searchTerm\":\"analgesic\"}")
      {
        status should equal (200)
      }
  }
  
  test("POST /medications/luceneMedLookup no params")
  {
      val res = post("/medications/luceneMedLookup")
      {
        status should equal (400)
      }
  }
  
  test("POST /medications/luceneMedLookup params with no results")
  {
      val res = post("/medications/luceneMedLookup", "{\"searchTerm\":\"not_a_med\"}")
      {
        status should equal (204)
      }
  }
  
  test("POST /medications/luceneMedLookup bad params")
  {
      val res = post("/medications/luceneMedLookup", "{bad_param}")
      {
        status should equal (400)
      }
  }
  
  test("POST /medications/findHopsAwayFromDrug with params")
  {
      val res = post("/medications/findHopsAwayFromDrug", "{\"searchList\":[\"http://purl.obolibrary.org/obo/CHEBI_23888\"]}")
      {
        status should equal (200)
      }
  }
  
  test("POST /medications/findHopsAwayFromDrug no params")
  {
      val res = post("/medications/findHopsAwayFromDrug")
      {
        status should equal (400)
      }
  }
  
  test("POST /medications/findHopsAwayFromDrug bad params")
  {
      val res = post("/medications/findHopsAwayFromDrug", "{bad_param}")
      {
        status should equal (400)
      }
  }
  
  test("GET /medications/lastGraphUpdate")
  {
      val res = get("/medications/lastGraphUpdate")
      {
        status should equal (200)
      }
  }
}
