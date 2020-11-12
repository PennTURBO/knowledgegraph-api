package edu.upenn.turbo

import org.neo4j.driver.AuthTokens
import org.neo4j.driver.GraphDatabase
import org.neo4j.driver.Values.parameters

import org.slf4j.LoggerFactory


class Neo4jCypherService(var uri: String, var user: String, var password: String)
{
	val driver = GraphDatabase.driver( uri, AuthTokens.basic( user, password ) )
	val logger = LoggerFactory.getLogger("turboAPIlogger")


	def close() =
	{
		driver.close()
	}

	/** Returns the context graph for a disease URI.

	Return a graphML formatted subgraph that shows how to get from the given 
	MONDO uri to the nodes that contain icd 9 or 10 codes.

	Will throw 'org.neo4j.driver.exceptions.ServiceUnavailableException' if the 
	neo4j server cannot be accessed.

	Will throw an IllegalArgumentException if the uri string is misformed.
	*/
    def getDiseaseContextGraphMl(uri: String): Option[String] =
    {
    	logger.info(s"getDiseaseContextGraphMl($uri)")
    	require(uri.startsWith("http://purl.obolibrary.org/obo/"), s"'$uri' is not a correctly formed MONDO url")

    	try {
	    	val cypher = generateDiseaseContextGraphCypher(uri)
	    	val session = driver.session()
	    	logger.debug(s"cypher: $cypher")
	    	
	    	val result = session.run(cypher)
	    	val record = result.single()
	    	logger.debug(s"query result: $record")
	    	logger.info(s"query result nodes: ${record.get("nodes").asObject()}")
	    	logger.info(s"query result relationships: ${record.get("relationships").asObject()}")
	    	logger.info(s"query result properties: ${record.get("properties").asObject()}")


	    	if (record.get("nodes").asInt() == 0) return None
	    	else return Some(record.get("data").asString())

    	} catch {
    		case e: Exception => 
	          {
	            logger.error("ERROR: exception when initilizing graphDb connections in DashboardServletTests.beforeAll() - ")
	            logger.error(e.toString)
	            throw e
	          }
    	}
    }


    /** Cypher to generate a context graph for a disease URI.

    Returns cypher to generate a graphML formatted subgraph 
    that shows how to get from the given MONDO uri to the 
    nodes that contain icd 9 or 10 codes.
    */
    def generateDiseaseContextGraphCypher(uri: String): String = {
    	val template = """
WITH "
	// Direct map from Mondo to ICD
	MATCH 
		(mondoParent:graphBuilder__mondoDiseaseClass{uri:$URI})<-[mondoParentEdge:rdfs__subClassOf]-
		(mondoChild:graphBuilder__mondoDiseaseClass)-[mondoChildEdge:mydata__mdbxr | skos__exactMatch | skos__closeMatch | owl__equivalentClass]->
		(icdParent)<-[icdParentEdge:rdfs__subClassOf]-
		(icdChild)
	WHERE 
		icdParent:graphBuilder__icd9Class OR icdParent:graphBuilder__icd10Class
		AND icdChild:graphBuilder__icd9Class OR icdChild:graphBuilder__icd10Class
	WITH  
		collect(mondoParent)+ 
		collect(mondoChild)+ 
		collect(icdParent)+ 
		collect(icdChild) as subgraphNodes,
		
		collect(mondoParentEdge)+ 
		collect(mondoChildEdge)+ 
		collect(icdParentEdge) as subgraphEdges
	RETURN subgraphNodes, subgraphEdges

	UNION

	// Mondo to ICD via CUI
	MATCH (
		mondoParent:graphBuilder__mondoDiseaseClass{uri:$URI})<-[mondoParentEdge:rdfs__subClassOf]-
		(mondoChild:graphBuilder__mondoDiseaseClass)-[mondoChildEdge:mydata__mdbxr | skos__exactMatch | skos__closeMatch | owl__equivalentClass]->
		(cui:mydata__materializedCui)<-[cuiEdge:mydata__materializedCui]-
		(icdParent)<-[icdParentEdge:rdfs__subClassOf]-
		(icdChild)
	WHERE 
		icdParent:graphBuilder__icd9Class OR icdParent:graphBuilder__icd10Class
		AND icdChild:graphBuilder__icd9Class OR icdChild:graphBuilder__icd10Class
	WITH  
		collect(mondoParent)+ 
		collect(mondoChild)+ 
		collect(icdParent)+ 
		collect(icdChild)+ 
		collect(cui) as subgraphNodes,

		collect(mondoParentEdge)+ 
		collect(mondoChildEdge)+ 
		collect(icdParentEdge)+
		collect(cuiEdge) as subgraphEdges
	RETURN subgraphNodes, subgraphEdges

	UNION

	// Mondo to ICD via Snomed and CUI
	MATCH (
		mondoParent:graphBuilder__mondoDiseaseClass{uri:$URI})<-[mondoParentEdge:rdfs__subClassOf]-
		(mondoChild:graphBuilder__mondoDiseaseClass)-[mondoChildEdge:mydata__mdbxr | skos__exactMatch | skos__closeMatch | owl__equivalentClass]->
		(snomedParent:graphBuilder__snomedDisorderClass)<-[snomedParentEdge:rdfs__subClassOf]-
		(snomedChild:graphBuilder__snomedDisorderClass)-[snomedChildEdge:mydata__materializedCui]->
		(cui:mydata__materializedCui)<-[cuiEdge:mydata__materializedCui]-
		(icdParent)<-[icdParentEdge:rdfs__subClassOf]-
		(icdChild)
	WHERE 
		icdParent:graphBuilder__icd9Class OR icdParent:graphBuilder__icd10Class
		AND icdChild:graphBuilder__icd9Class OR icdChild:graphBuilder__icd10Class
	WITH  
		collect(mondoParent)+ 
		collect(mondoChild)+ 
		collect(icdParent)+ 
		collect(icdChild)+ 
		collect(cui)+ 
		collect(snomedParent)+ 
		collect(snomedChild) as subgraphNodes,

		collect(mondoParentEdge)+ 
		collect(mondoChildEdge)+ 
		collect(icdParentEdge)+ 
		collect(cuiEdge)+ 
		collect(snomedParentEdge)+ 
		collect(snomedChildEdge) as subgraphEdges
	RETURN subgraphNodes, subgraphEdges

	UNION

	// Mondo to ICD9 via SNOMED and NLM
	MATCH (
		mondoParent:graphBuilder__mondoDiseaseClass{uri:$URI})<-[mondoParentEdge:rdfs__subClassOf]-
		(mondoChild:graphBuilder__mondoDiseaseClass)-[mondoChildEdge:mydata__mdbxr | skos__exactMatch | skos__closeMatch | owl__equivalentClass]->
		(snomedParent:graphBuilder__snomedDisorderClass)<-[snomedParentEdge:rdfs__subClassOf]-
		(snomedChild:graphBuilder__snomedDisorderClass)<-[snomedChildEdge:`umls_mapping__icd9cm_to_snomedct.html`]-
		(icdParent:graphBuilder__icd9Class)<-[icdParentEdge:rdfs__subClassOf]-
		(icdChild:graphBuilder__icd9Class)
	WITH 
		collect(mondoParent)+
		collect(mondoChild)+
		collect(icdParent)+
		collect(icdChild)+
		collect(snomedParent)+
		collect(snomedChild) as subgraphNodes,

		collect(mondoParentEdge)+ 
		collect(mondoChildEdge)+ 
		collect(icdParentEdge)+ 
		collect(snomedParentEdge)+ 
		collect(snomedChildEdge) as subgraphEdges
	RETURN subgraphNodes, subgraphEdges " AS query

CALL apoc.export.graphml.query(query, null, {stream:true})
YIELD file, nodes, relationships, properties, data
RETURN file, nodes, relationships, properties, data;
"""

	val cypher = template.replaceAll("\\$URI", "'" + uri + "'")
	return cypher
    }
}
