import edu.upenn.turbo._
import org.scalatra._
import javax.servlet.ServletContext

import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer
import java.io.File

import org.neo4j.graphdb._
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager

class ScalatraBootstrap extends LifeCycle with DashboardProperties {

  override def destroy(context: ServletContext) 
  {
      /*val neo4jgraph = Neo4jGraphConnection.getGraph()
      neo4jgraph.close()*/

      Neo4jCypherServiceHolder.getService().close()
      

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

  override def init(context: ServletContext) {

    //establish connections to graph databases
    /*var neo4jgraph: Neo4jGraph = null
    println("connecting to neo4j...")
    try 
    {
        neo4jgraph = Neo4jGraph.open("neo4j.graph") 
    } 
    catch 
    {
        case e: Throwable => e.printStackTrace()
    }
    
    println("established neo4j connection")
    Neo4jGraphConnection.setGraph(neo4jgraph)*/

    println("setting up neo4j cypher service...")
    val neo4jCypherService = new Neo4jCypherService(
      getFromProperties("neo4j_url"), 
      getFromProperties("neo4j_username"), 
      getFromProperties("neo4j_password")
    ) 
    Neo4jCypherServiceHolder.setService(neo4jCypherService)

    println("connecting to graph db...")

    val diagRepoManager = new RemoteRepositoryManager(getFromProperties("serviceURL"))
    diagRepoManager.setUsernameAndPassword(getFromProperties("username"), getFromProperties("password"))
    diagRepoManager.initialize()
    val diagRepository = diagRepoManager.getRepository(getFromProperties("diagnoses_repository"))
    val diagCxn = diagRepository.getConnection()

    GraphDbConnection.setDiagRepoManager(diagRepoManager)
    GraphDbConnection.setDiagRepository(diagRepository)
    GraphDbConnection.setDiagConnection(diagCxn)

    println("established connection to diagnoses repository")

    val medRepoManager = new RemoteRepositoryManager(getFromProperties("serviceURL"))
    medRepoManager.setUsernameAndPassword(getFromProperties("username"), getFromProperties("password"))
    medRepoManager.initialize()
    val medRepository = medRepoManager.getRepository(getFromProperties("medications_repository"))
    val medCxn = medRepository.getConnection()

    GraphDbConnection.setMedRepoManager(medRepoManager)
    GraphDbConnection.setMedRepository(medRepository)
    GraphDbConnection.setMedConnection(medCxn)

    println("established connection to medications repository")

    val ontRepoManager = new RemoteRepositoryManager(getFromProperties("serviceURL"))
    ontRepoManager.setUsernameAndPassword(getFromProperties("username"), getFromProperties("password"))
    ontRepoManager.initialize()
    val ontRepository = ontRepoManager.getRepository(getFromProperties("ontology_repository"))
    val ontCxn = ontRepository.getConnection()

    GraphDbConnection.setOntRepoManager(ontRepoManager)
    GraphDbConnection.setOntRepository(ontRepository)
    GraphDbConnection.setOntConnection(ontCxn)

    println("established connection to ontologies repository")

    val medMapRepoManager = new RemoteRepositoryManager(getFromProperties("serviceURL"))
    ontRepoManager.setUsernameAndPassword(getFromProperties("username"), getFromProperties("password"))
    ontRepoManager.initialize()
    val medMapRepository = medMapRepoManager.getRepository(getFromProperties("medMap_ontology_repository"))
    val medMapCxn = medMapRepository.getConnection()

    GraphDbConnection.setMedMapRepoManager(medMapRepoManager)
    GraphDbConnection.setMedMapRepository(medMapRepository)
    GraphDbConnection.setMedMapConnection(medMapCxn)

    println("established connection to medMap repository")

    println("established all graph db connections")

    context.mount(new DashboardServlet, "/*")
  	println("""


 .----------------.  .----------------.  .----------------.  .----------------.  .----------------.   
| .--------------. || .--------------. || .--------------. || .--------------. || .--------------. |  
| |  ________    | || |      __      | || |    _______   | || |  ____  ____  | || |              | |  
| | |_   ___ `.  | || |     /  \     | || |   /  ___  |  | || | |_   ||   _| | || |              | |  
| |   | |   `. \ | || |    / /\ \    | || |  |  (__ \_|  | || |   | |__| |   | || |    ______    | |  
| |   | |    | | | || |   / ____ \   | || |   '.___`-.   | || |   |  __  |   | || |   |______|   | |  
| |  _| |___.' / | || | _/ /    \ \_ | || |  |`\____) |  | || |  _| |  | |_  | || |              | |  
| | |________.'  | || ||____|  |____|| || |  |_______.'  | || | |____||____| | || |              | |  
| |              | || |              | || |              | || |              | || |              | |  
| '--------------' || '--------------' || '--------------' || '--------------' || '--------------' |  
 '----------------'  '----------------'  '----------------'  '----------------'  '----------------'   
 .----------------.  .----------------.  .----------------.  .----------------.  .----------------.   
| .--------------. || .--------------. || .--------------. || .--------------. || .--------------. |  
| |   ______     | || |     ____     | || |      __      | || |  _______     | || |  ________    | |  
| |  |_   _ \    | || |   .'    `.   | || |     /  \     | || | |_   __ \    | || | |_   ___ `.  | |  
| |    | |_) |   | || |  /  .--.  \  | || |    / /\ \    | || |   | |__) |   | || |   | |   `. \ | |  
| |    |  __'.   | || |  | |    | |  | || |   / ____ \   | || |   |  __ /    | || |   | |    | | | |  
| |   _| |__) |  | || |  \  `--'  /  | || | _/ /    \ \_ | || |  _| |  \ \_  | || |  _| |___.' / | |  
| |  |_______/   | || |   `.____.'   | || ||____|  |____|| || | |____| |___| | || | |________.'  | |  
| |              | || |              | || |              | || |              | || |              | |  
| '--------------' || '--------------' || '--------------' || '--------------' || '--------------' |  
 '----------------'  '----------------'  '----------------'  '----------------'  '----------------'   
                   .----------------.  .----------------.  .----------------.                         
                  | .--------------. || .--------------. || .--------------. |                        
                  | |      __      | || |   ______     | || |     _____    | |                        
                  | |     /  \     | || |  |_   __ \   | || |    |_   _|   | |                        
                  | |    / /\ \    | || |    | |__) |  | || |      | |     | |                        
                  | |   / ____ \   | || |    |  ___/   | || |      | |     | |                        
                  | | _/ /    \ \_ | || |   _| |_      | || |     _| |_    | |                        
                  | ||____|  |____|| || |  |_____|     | || |    |_____|   | |                        
                  | |              | || |              | || |              | |                        
                  | '--------------' || '--------------' || '--------------' |                        
                   '----------------'  '----------------'  '----------------'                         
 .----------------.  .----------------.  .----------------.  .----------------.  .----------------.   
| .--------------. || .--------------. || .--------------. || .--------------. || .--------------. |  
| |     ____     | || |              | || |     ____     | || |              | || |    _____     | |  
| |   .'    '.   | || |              | || |   .'    '.   | || |              | || |   / ___ `.   | |  
| |  |  .--.  |  | || |              | || |  |  .--.  |  | || |              | || |  |_/___) |   | |  
| |  | |    | |  | || |              | || |  | |    | |  | || |              | || |   .'____.'   | |  
| |  |  `--'  |  | || |      _       | || |  |  `--'  |  | || |      _       | || |  / /____     | |  
| |   '.____.'   | || |     (_)      | || |   '.____.'   | || |     (_)      | || |  |_______|   | |  
| |              | || |              | || |              | || |              | || |              | |  
| '--------------' || '--------------' || '--------------' || '--------------' || '--------------' |  
 '----------------'  '----------------'  '----------------'  '----------------'  '----------------'   
                                                                                 
""")
  }
}
