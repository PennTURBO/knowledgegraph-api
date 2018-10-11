package edu.upenn.turbo

import org.scalatra.test.scalatest._

class drivetrainWebServletTests extends ScalatraFunSuite {

  addServlet(classOf[DashboardServlet], "/*")

  test("GET / on dashboardServlet should return status 200") {
    get("/") {
      status should equal (200)
    }
  }

}
