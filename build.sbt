val ScalatraVersion = "2.6.3"

organization := "edu.upenn"

name := "Dashboard"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.12.6"

resolvers += Classpaths.typesafeReleases

libraryDependencies ++= Seq(
  "org.scalatra" %% "scalatra" % ScalatraVersion,
  "org.scalatra" %% "scalatra-scalatest" % ScalatraVersion % "test",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % "runtime",
  "org.eclipse.jetty" % "jetty-webapp" % "9.4.9.v20180320" % "container",
  "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided",
  "org.eclipse.rdf4j" % "rdf4j-runtime" % "2.4.0-M1",
  "org.json4s" % "json4s-jackson_2.12" % "3.5.2",
  "org.scalatra" % "scalatra-json_2.12" % "2.6.3"
)

enablePlugins(SbtTwirl)
enablePlugins(ScalatraPlugin)
