package edu.upenn.turbo

import java.util.Properties
import java.io.FileInputStream

trait DashboardProperties {

  def getFromProperties(property: String): String = 
  {
    val input: FileInputStream = new FileInputStream("turboAPI.properties")
    val props: Properties = new Properties()
    props.load(input)
    input.close()
    props.getProperty(property)
  }
}
