package edu.upenn.turbo

import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph
import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager

/*object Neo4jGraphConnection
{
	var neo4jgraph: Neo4jGraph = null

	def setGraph(neo4jgraph: Neo4jGraph)
	{
		this.neo4jgraph = neo4jgraph
	}

	def getGraph(): Neo4jGraph = neo4jgraph
}*/

object Neo4jCypherServiceHolder
{
    var neo4jCypherService: Neo4jCypherService = null

    def setService(neo4jCypherService: Neo4jCypherService)
    {
        this.neo4jCypherService = neo4jCypherService
    }

    def getService(): Neo4jCypherService = neo4jCypherService
}

object GraphDbConnection
{
	var diagCxn: RepositoryConnection = null
    var diagRepo: Repository = null
    var diagRepoManager: RemoteRepositoryManager = null

    var medCxn: RepositoryConnection = null
    var medRepo: Repository = null
    var medRepoManager: RemoteRepositoryManager = null

    var ontCxn: RepositoryConnection = null
    var ontRepo: Repository = null
    var ontRepoManager: RemoteRepositoryManager = null

    var medMapCxn: RepositoryConnection = null
    var medMapRepo: Repository = null
    var medMapRepoManager: RemoteRepositoryManager = null

	def setDiagConnection(diagCxn: RepositoryConnection)
    {
        this.diagCxn = diagCxn
    }
    
    def getDiagConnection(): RepositoryConnection = diagCxn
    
    def setDiagRepository(diagRepo: Repository)
    {
        this.diagRepo = diagRepo
    }
    
    def getDiagRepository(): Repository = diagRepo
    
    def setDiagRepoManager(diagRepoManager: RemoteRepositoryManager)
    {
        this.diagRepoManager = diagRepoManager
    }
    
    def getDiagRepoManager(): RemoteRepositoryManager = diagRepoManager

    def setMedConnection(medCxn: RepositoryConnection)
    {
        this.medCxn = medCxn
    }
    
    def getMedConnection(): RepositoryConnection = medCxn
    
    def setMedRepository(medRepo: Repository)
    {
        this.medRepo = medRepo
    }
    
    def getMedRepository(): Repository = medRepo
    
    def setMedRepoManager(medRepoManager: RemoteRepositoryManager)
    {
        this.medRepoManager = medRepoManager
    }
    
    def getMedRepoManager(): RemoteRepositoryManager = medRepoManager

    def setOntConnection(ontCxn: RepositoryConnection)
    {
        this.ontCxn = ontCxn
    }
    
    def getOntConnection(): RepositoryConnection = ontCxn
    
    def setOntRepository(ontRepo: Repository)
    {
        this.ontRepo = ontRepo
    }
    
    def getOntRepository(): Repository = ontRepo
    
    def setOntRepoManager(ontRepoManager: RemoteRepositoryManager)
    {
        this.ontRepoManager = ontRepoManager
    }
    
    def getOntRepoManager(): RemoteRepositoryManager = ontRepoManager

    def setMedMapConnection(medMapCxn: RepositoryConnection)
    {
        this.medMapCxn = medMapCxn
    }
    
    def getMedMapConnection(): RepositoryConnection = medMapCxn
    
    def setMedMapRepository(medMapRepo: Repository)
    {
        this.medMapRepo = medMapRepo
    }
    
    def getMedMapRepository(): Repository = medMapRepo
    
    def setMedMapRepoManager(medMapRepoManager: RemoteRepositoryManager)
    {
        this.medMapRepoManager = medMapRepoManager
    }
    
    def getMedMapRepoManager(): RemoteRepositoryManager = medMapRepoManager
}