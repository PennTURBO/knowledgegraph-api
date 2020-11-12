package edu.upenn.turbo

import org.scalatra.test.scalatest._
import org.scalatest.BeforeAndAfterAll
import org.scalatest._


class Neo4jCypherServiceTests extends ScalatraFunSuite with BeforeAndAfterAll with DashboardProperties
{

  var cypherService: Neo4jCypherService = null

  override def beforeAll()
  {
      super.beforeAll()
      cypherService = new Neo4jCypherService()
  }

  test("getDiseaseContextGraphMl good") 
  {
  	  var iri = "http://purl.obolibrary.org/obo/MONDO_0005149"

  	  var expectedStart = """<?xml version="1.0" encoding="UTF-8"?>"""
  	  var expectedInclude = """skos__notation"""

  	  var res = cypherService.getDiseaseContextGraphMl(iri)
      
      res should include(expectedInclude)
      res should include(iri)
      res should startWith(expectedStart)
  }
}
