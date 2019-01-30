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
  
  test("POST /medications/medicationTextSearch with params")
  {
      val res = post("/medications/medicationTextSearch", "{\"searchTerm\":\"analgesic\"}")
      {
        status should equal (200)
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
