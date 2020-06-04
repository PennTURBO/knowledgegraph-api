lazy val commonSettings = Seq(
  version := "0.0.2-SNAPSHOT",
  organization := "edu.upenn",
  scalaVersion := "2.11.8",
  scalaVersion in ThisBuild := "2.11.8",
  test in assembly := {},
  name := "Turbo-API"
)

val ScalatraVersion = "2.6.3"

lazy val app = (project in file("app")).
  settings(commonSettings: _*).
  settings(
    mainClass in assembly := Some("edu.upenn.turbo.JettyLauncher"),
  )

lazy val utils = (project in file("utils")).
  settings(commonSettings: _*).
  settings(
    assemblyJarName in assembly := "turboAPI.jar",
  )

resolvers += Resolver.mavenLocal

assemblyMergeStrategy in assembly := {
	case PathList("README.txt") => MergeStrategy.discard
    case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
    case PathList("META-INF", "LICENSES.txt") => MergeStrategy.discard
    case PathList("META-INF", "DEPENDENCIES") => MergeStrategy.discard
    case PathList("META-INF", "LICENSE") => MergeStrategy.discard
    case PathList("META-INF", "LICENSE.txt") => MergeStrategy.discard
    case PathList("META-INF", "NOTICE") => MergeStrategy.discard
    case PathList("META-INF", "NOTICE.txt") => MergeStrategy.discard
    case PathList("META-INF", "README.md") => MergeStrategy.discard
    case PathList("META-INF", "README.txt") => MergeStrategy.discard
    case PathList("META-INF", "maven", "com.fasterxml.jackson.core", "jackson-core", "pom.xml") => MergeStrategy.discard
    case PathList("META-INF", "maven", "com.fasterxml.jackson.core", "jackson-core", "pom.properties") => MergeStrategy.discard
    case PathList("META-INF", "maven", "com.fasterxml.jackson.core", "jackson-databind", "pom.xml") => MergeStrategy.discard
    case PathList("META-INF", "maven", "com.fasterxml.jackson.core", "jackson-databind", "pom.properties") => MergeStrategy.discard
    case PathList("META-INF", "modules.properties") => MergeStrategy.discard
    case PathList("META-INF", "services", "javax.annotation.processing.Processor") => MergeStrategy.concat
    case PathList("META-INF", "services", "org.apache.lucene.codecs.Codec") => MergeStrategy.concat
    case PathList("META-INF", "services", "org.eclipse.rdf4j.repository.config.RepositoryFactory") => MergeStrategy.concat
    case PathList("META-INF", "services", "org.eclipse.rdf4j.rio.RDFParserFactory") => MergeStrategy.concat
    case PathList("META-INF", "services", "org.eclipse.rdf4j.rio.RDFWriterFactory") => MergeStrategy.concat
    case PathList("META-INF", "services", "org.neo4j.jmx.impl.ManagementBeanProvider") => MergeStrategy.concat
    case PathList("META-INF", "services", "org.neo4j.kernel.KernelExtension") => MergeStrategy.concat
    case PathList("META-INF", "services", "org.neo4j.kernel.api.security.SecurityModule") => MergeStrategy.concat
    case PathList("META-INF", "services", "org.neo4j.kernel.impl.transaction.xaframework.TransactionInterceptorProvider") => MergeStrategy.concat
    case PathList("META-INF", "services", "org.apache.lucene.codecs.DocValuesFormat") => MergeStrategy.concat
    case PathList("META-INF", "services", "org.apache.lucene.codecs.PostingsFormat") => MergeStrategy.concat
    case PathList("META-INF", "services", "org.neo4j.commandline.admin.AdminCommand$Provider") => MergeStrategy.concat
    case PathList("META-INF", "services", "org.neo4j.configuration.LoadableConfig") => MergeStrategy.concat
    case PathList("META-INF", "services", "org.neo4j.kernel.extension.KernelExtensionFactory") => MergeStrategy.concat
    case PathList("org", "slf4j", "impl", "StaticLoggerBinder.class") => MergeStrategy.first
    case PathList("org", "slf4j", "impl", "StaticMDCBinder.class") => MergeStrategy.first
    case PathList("org", "slf4j", "impl", "StaticMarkerBinder.class") => MergeStrategy.first
    case PathList("META-INF", xs @ _*) =>
      (xs map {_.toLowerCase}) match {
    case ps @ (x :: xs) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") || ps.last.endsWith(".rsa") =>
          MergeStrategy.discard
    case x => MergeStrategy.deduplicate
    }
  case x => MergeStrategy.deduplicate
}

resolvers += Classpaths.typesafeReleases

libraryDependencies ++= Seq(
  "org.scalatra" %% "scalatra" % ScalatraVersion,
  "org.scalatra" %% "scalatra-scalatest" % ScalatraVersion % "test",
  "org.pegdown" % "pegdown" % "1.6.0" % Test, // for html reports, see <https://stackoverflow.com/questions/37056570/how-to-generate-html-reports-on-play-scalatest>
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.eclipse.jetty" % "jetty-webapp" % "9.4.9.v20180320" % "container;compile",
  "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided",
  "org.json4s" % "json4s-jackson_2.11" % "3.5.2",
  "org.scalatra" % "scalatra-json_2.11" % "2.6.3",

  //Neo4j
  "org.apache.tinkerpop" % "neo4j-gremlin" % "3.3.1",
  "org.neo4j" % "neo4j-tinkerpop-api-impl" % "0.7-3.2.3",

  //RDF4J
  "org.eclipse.rdf4j" % "rdf4j-model" % "2.4.0-M1",
  "org.eclipse.rdf4j" % "rdf4j-repository-api" % "2.4.0-M1",
  "org.eclipse.rdf4j" % "rdf4j-repository-manager" % "2.4.0-M1",

  //Solr
  "com.github.takezoe" %% "solr-scala-client" % "0.0.24",

  //"edu.upenn.pmbb" % "carnival-util" % "0.2.0"
)

enablePlugins(SbtTwirl)
enablePlugins(ScalatraPlugin)

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/test-reports/html")