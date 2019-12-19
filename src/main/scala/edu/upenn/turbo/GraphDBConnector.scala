package edu.upenn.turbo

import java.io.File
import org.neo4j.graphdb._
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.out
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.in
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.has
import org.apache.tinkerpop.gremlin.process.traversal.P._
import org.eclipse.rdf4j.query.QueryLanguage
import org.eclipse.rdf4j.query.TupleQuery
import org.eclipse.rdf4j.query.TupleQueryResult
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.eclipse.rdf4j.query.BooleanQuery
import org.eclipse.rdf4j.query.BindingSet
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.OpenRDFException
import org.eclipse.rdf4j.model.Statement
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import org.eclipse.rdf4j.query.QueryEvaluationException

import java.nio.file.Path
import java.nio.file.Paths
import org.slf4j.LoggerFactory

class GraphDBConnector 
{
    val logger = LoggerFactory.getLogger("turboAPIlogger")

    def getDiseaseURIs(startingCodes: Array[String], cxn: RepositoryConnection): Array[HashMap[String,String]] =
    {
        var resultList = new ArrayBuffer[HashMap[String,String]]
        val chunkedListsItr = startingCodes.grouped(10000)
        while (chunkedListsItr.hasNext)
        {
            val startingCodes = chunkedListsItr.next()
            var startListAsString = ""
            for (code <- startingCodes) startListAsString += " <" + code + "> "
            logger.info("launching query to Graph DB")
            val query = s"""
                PREFIX obo: <http://purl.obolibrary.org/obo/>
                PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                PREFIX graphBuilder: <http://graphBuilder.org/>
                PREFIX pmbb: <http://www.itmat.upenn.edu/biobank/>
                select ?mondoSub ?mondoLabel ?rare ?syndromic ?congenital ?pathFamily ?assertionOrientation ?assertedPredicate ?icdLeaf ?icdVer ?icdCode ?icdLabel where
                {
                    Values ?icdLeaf {$startListAsString}
                    graph pmbb:cached_mondo_icd_mappings_LEAFONLY
                    {
                        ?mapItem a graphBuilder:cachedMapping .
                        ?mapItem graphBuilder:hasMondoTerm ?mondoSub .
                        ?mapItem graphBuilder:hasIcdTerm ?icdLeaf .
                        ?mapItem graphBuilder:mondoLabel ?mondoLabel .
                        ?mapItem graphBuilder:pathFamily ?pathFamily .
                        ?mapItem graphBuilder:assertionOrientation ?assertionOrientation .
                        ?mapItem graphBuilder:assertedPredicate ?assertedPredicate .
                        ?mapItem graphBuilder:icdVersion ?icdVer .
                        ?mapItem graphBuilder:icdAsString ?icdCode .
                        ?mapItem graphBuilder:icdLabel ?icdLabel .
                        ?mapItem graphBuilder:isRare ?rare .
                        ?mapItem graphBuilder:isCongenital ?congenital .
                        ?mapItem graphBuilder:isSyndromic ?syndromic .
                    }
                }
                Order By ?mondoSub ?icdLeaf"""

            println(query)    

            val tupleQueryResult = cxn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()
            while (tupleQueryResult.hasNext()) 
            {
                val singleResultMap = new HashMap[String,String]
                val bindingset: BindingSet = tupleQueryResult.next()
                singleResultMap += "MondoTerm" -> bindingset.getValue("mondoSub").toString
                singleResultMap += "MondoLabel" -> bindingset.getValue("mondoLabel").toString
                singleResultMap += "rare" -> bindingset.getValue("rare").toString
                singleResultMap += "syndromic" -> bindingset.getValue("syndromic").toString
                singleResultMap += "congenital" -> bindingset.getValue("congenital").toString
                singleResultMap += "PathFamily" -> bindingset.getValue("pathFamily").toString
                singleResultMap += "AssertionOrientation" -> bindingset.getValue("assertionOrientation").toString
                singleResultMap += "AssertedPredicate" -> bindingset.getValue("assertedPredicate").toString
                singleResultMap += "Icd" -> bindingset.getValue("icdLeaf").toString
                singleResultMap += "IcdVersion" -> bindingset.getValue("icdVer").toString
                singleResultMap += "IcdCode" -> bindingset.getValue("icdCode").toString
                singleResultMap += "IcdLabel" -> bindingset.getValue("icdLabel").toString
                resultList += singleResultMap
            }
          }
        
        logger.info("result size: " + resultList.size)
        resultList.toArray
    }

    def getSemanticContextForDiseaseURIs(startingCodes: Array[String], cxn: RepositoryConnection): Array[Array[String]] =
    {
        var startListAsString = ""
        for (code <- startingCodes) startListAsString += " <" + code + "> "
        logger.info("launching query to Graph DB")

        val query = s"""
        PREFIX rdf: <http://www.w3.org/2000/01/rdf-schema#>
        PREFIX : <http://purl.obolibrary.org/obo/mondo.owl#>
        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        CONSTRUCT {
            ?target rdf:subClassOf ?sub1 .
            ?sub1 rdf:subClassOf ?sub2 .
            #?target <http://heath/example/option> True .
            ?target <http://graphBuilder.org/mapsTo> ?code .
            ?code skos:prefLabel ?codeLabel .
            ?code skos:notation ?notation .
            ?target rdfs:label ?targetLabel .
            ?sub1 rdfs:label ?sub1Label .
            ?sub2 rdfs:label ?sub2Label .
        }
        WHERE {
            values ?target {$startListAsString}
            {
                ?target <http://graphBuilder.org/mapsTo> ?code .
                ?code skos:prefLabel ?codeLabel .
                ?code skos:notation ?notation 
            } 
            {
                ?target rdf:subClassOf ?sub1 .
                ?target rdfs:label ?targetLabel .
                ?sub1 rdfs:label ?sub1Label .
                OPTIONAL {
                    ?sub1 rdf:subClassOf ?sub2 .
                    ?sub2 rdfs:label ?sub2Label .
                }
            }
        }
        """

        val queryResult = cxn.prepareGraphQuery(query).evaluate()
        
        val resultList: ArrayBuffer[Array[String]] = new ArrayBuffer[Array[String]]
        while (queryResult.hasNext()) 
        {
            val statement: Statement = queryResult.next()
            val subject: String = statement.getSubject().toString()
            val predicate: String = statement.getPredicate().toString()
            val obj: String = statement.getObject().toString()

            resultList += Array(subject, predicate, obj)
        }
        logger.info("result size: " + resultList.size)
        logger.info("result: " + resultList)
        resultList.toArray
    }

    def getDiagnosisCodes(start: String, cxn: RepositoryConnection): Array[HashMap[String,String]] =
    {
        val query = s"""
        PREFIX obo: <http://purl.obolibrary.org/obo/>
        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
        PREFIX pmbb: <http://www.itmat.upenn.edu/biobank/>
        prefix graphBuilder: <http://graphBuilder.org/>
        select ?mondoSub ?mondoLabel ?rare ?syndromic ?congenital ?pathFamily ?assertionOrientation ?assertedPredicate ?icdLeaf ?icdVer ?icdCode ?icdLabel where
        {
            graph <http://example.com/resource/MondoTransitiveSubClasses> {
                ?mondoSub rdfs:subClassOf <$start> .
            }
            graph pmbb:cached_mondo_icd_mappings_LEAFONLY
            {
                ?mapItem a graphBuilder:cachedMapping .
                ?mapItem graphBuilder:hasMondoTerm ?mondoSub .
                ?mapItem graphBuilder:hasIcdTerm ?icdLeaf .
                ?mapItem graphBuilder:mondoLabel ?mondoLabel .
                ?mapItem graphBuilder:pathFamily ?pathFamily .
                ?mapItem graphBuilder:assertionOrientation ?assertionOrientation .
                ?mapItem graphBuilder:assertedPredicate ?assertedPredicate .
                ?mapItem graphBuilder:icdVersion ?icdVer .
                ?mapItem graphBuilder:icdAsString ?icdCode .
                ?mapItem graphBuilder:icdLabel ?icdLabel .
                ?mapItem graphBuilder:isRare ?rare .
                ?mapItem graphBuilder:isCongenital ?congenital .
                ?mapItem graphBuilder:isSyndromic ?syndromic .
            }
        }
        Order By ?mondoSub ?icdLeaf
        """
        println(query)    

        val resultList = new ArrayBuffer[HashMap[String,String]]
        val tupleQueryResult = cxn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()
        while (tupleQueryResult.hasNext()) 
        {
            val singleResultMap = new HashMap[String,String]
            val bindingset: BindingSet = tupleQueryResult.next()
            singleResultMap += "MondoTerm" -> bindingset.getValue("mondoSub").toString
            singleResultMap += "MondoLabel" -> bindingset.getValue("mondoLabel").toString
            singleResultMap += "rare" -> bindingset.getValue("rare").toString
            singleResultMap += "syndromic" -> bindingset.getValue("syndromic").toString
            singleResultMap += "congenital" -> bindingset.getValue("congenital").toString
            singleResultMap += "PathFamily" -> bindingset.getValue("pathFamily").toString
            singleResultMap += "AssertionOrientation" -> bindingset.getValue("assertionOrientation").toString
            singleResultMap += "AssertedPredicate" -> bindingset.getValue("assertedPredicate").toString
            singleResultMap += "Icd" -> bindingset.getValue("icdLeaf").toString
            singleResultMap += "IcdVersion" -> bindingset.getValue("icdVer").toString
            singleResultMap += "IcdCode" -> bindingset.getValue("icdCode").toString
            singleResultMap += "IcdLabel" -> bindingset.getValue("icdLabel").toString
            resultList += singleResultMap
        }

        logger.info("result size: " + resultList.size)
        resultList.toArray
    }

    def getMedicationFullNameResults(startList: Array[String], cxn: RepositoryConnection): HashMap[String, ArrayBuffer[String]] =
    {
        var startListAsString = ""
        for (med <- startList) startListAsString += " <" + med + "> "
        val query = s"""

        PREFIX mydata: <http://example.com/resource/>
        select distinct ?start ?fullName
        where {
           values ?start {$startListAsString}
           ?s mydata:inherits_from ?start .
           graph     mydata:mdm_ods_meds_20180403_unique_cols.csv
           {
               ?s  mydata:FULL_NAME ?fullName
           }
           FILTER (!REGEX(STR(?fullName), "\u001a"))
        }
      """
      val tupleQueryResult = cxn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()
      val resultMap = new HashMap[String, ArrayBuffer[String]]
      while (tupleQueryResult.hasNext()) 
      {
          val bindingset: BindingSet = tupleQueryResult.next()
          var fullName: String = bindingset.getValue("fullName").toString
          var start: String = bindingset.getValue("start").toString
          if (resultMap.contains(start)) resultMap(start) += fullName
          else resultMap += start -> ArrayBuffer(fullName)
      }

      logger.info("result size: " + resultMap.size)
      resultMap
    }

    def getBestMatchTermForMedicationLookup(cxn: RepositoryConnection, userInput: String, limit: Integer = 1): Option[ArrayBuffer[ArrayBuffer[String]]] =
    {
      val query = """
        PREFIX : <http://www.ontotext.com/connectors/lucene#>
        PREFIX inst: <http://www.ontotext.com/connectors/lucene/instance#>
        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        SELECT ?entity ?score ?label {
            ?search a inst:role_via_rdfs_or_skos_label ;
                    :query "role_via_rdfs_label:"""+userInput+""" OR role_via_skos_label:"""+userInput+"""" ;
                                                       :entities ?entity .
            ?entity :score ?score .
            {
                {
                    graph <http://data.bioontology.org/ontologies/RXNORM/submissions/15/download> 
                    {
                        ?entity skos:prefLabel ?label .
                    }
                }
                union
                {
                    graph <ftp://ftp.ebi.ac.uk/pub/databases/chebi/ontology/chebi_lite.owl.gz>
                    {
                        ?entity rdfs:label ?label .
                    }
                }
            }
        }
        order by desc(?score)
        limit 
        """ + limit

        val tupleQueryResult = cxn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()
        var buffResults = new ArrayBuffer[ArrayBuffer[String]]
        while (tupleQueryResult.hasNext()) 
        {
            val nextResult = tupleQueryResult.next
            val singleResult = ArrayBuffer(nextResult.getValue("entity").toString, nextResult.getValue("label").toString)
            buffResults += singleResult
        }
        if (buffResults.size != 0) Some(buffResults)
        else None
      }

      def getBestMatchTermForDiagnosisLookup(cxn: RepositoryConnection, userInput: String, limit: Integer = 1): Option[ArrayBuffer[ArrayBuffer[String]]] =
      {
          val query = """
          PREFIX : <http://www.ontotext.com/connectors/lucene#>
          PREFIX inst: <http://www.ontotext.com/connectors/lucene/instance#>
          PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
          PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
          SELECT ?entity ?score ?label {
              ?search a inst:MonDO_labelsAndSynonyms ;
                      :query "mondoLabel:"""+userInput+""" OR mondoExactSynonym:"""+userInput+"""" ;
                                                 :entities ?entity .
              ?entity :score ?score .
              ?entity rdfs:label ?label .
          }
          order by desc(?score)
          limit """ + limit

          val tupleQueryResult = cxn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()
          var buffResults = new ArrayBuffer[ArrayBuffer[String]]
          while (tupleQueryResult.hasNext()) 
          {
              val nextResult = tupleQueryResult.next
              val singleResult = ArrayBuffer(nextResult.getValue("entity").toString, nextResult.getValue("label").toString)
              buffResults += singleResult
          }
          if (buffResults.size != 0) Some(buffResults)
          else None
      }

      def getOmopConceptMap(cxn: RepositoryConnection): Map[String,String] =
      {
          val sparql = s"""

              PREFIX turbo: <http://transformunify.org/ontologies/>
              PREFIX pmbb: <http://www.itmat.upenn.edu/biobank/>
              select ?uri ?conceptId where
              {
                 graph <https://raw.githubusercontent.com/PennTURBO/Turbo-Ontology/master/ontologies/turbo_merged.owl>
                 {
                     ?uri turbo:TURBO_0010147 ?conceptId .
                     filter (?conceptId != 0)
                 }
              }

          """
          var resMap = new HashMap[String, String]
          val tupleQueryResult = cxn.prepareTupleQuery(QueryLanguage.SPARQL, sparql).evaluate()
          while (tupleQueryResult.hasNext())
          {
              val nextItem = tupleQueryResult.next
              val uri = nextItem.getValue("uri").toString
              val conceptId = nextItem.getValue("conceptId").toString
              resMap += conceptId -> uri
          }
          if (resMap.size == 0) logger.info("no concept ID mappings found in TURBO ontology")
          resMap.toMap
      }

      def getDiagnosisMappingPathways(cxn: RepositoryConnection): Array[String] =
      {
          val query = s"""
            PREFIX obo: <http://purl.obolibrary.org/obo/>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX owl: <http://www.w3.org/2002/07/owl#>
            PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
            PREFIX snomed: <http://purl.bioontology.org/ontology/SNOMEDCT/>
            PREFIX umls: <http://bioportal.bioontology.org/ontologies/umls/>
            PREFIX oboInOwl: <http://www.geneontology.org/formats/oboInOwl#>
            PREFIX turbo: <http://transformunify.org/ontologies/>

            select distinct ?method where
            {
                ?g <http://graphBuilder.org/usedMethod> ?method .
            }
          """
          var resBuffer = new ArrayBuffer[String]
          val tupleQueryResult = cxn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()
          while (tupleQueryResult.hasNext())
          {
              val nextItem = tupleQueryResult.next
              val method = nextItem.getValue("method").toString.replaceAll("\"","")
              resBuffer += method
          }
          if (resBuffer.size == 0) logger.info("no concept ID mappings found in TURBO ontology")
          resBuffer.toArray
      }
}