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

    def getDiseaseURIs(startingCodes: Array[String], filterMethod: String, cxn: RepositoryConnection): Array[HashMap[String,String]] =
    {
        var graphName = "pmbb:cached_mondo_icd_mappings"
        if (filterMethod == "LEAF") graphName += "_LEAFONLY"
        else if (filterMethod == "REVERSEMAPS_DEPTH_FORMULA") graphName += "_depthTimesMappingCountFormulaOnly"
        
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
                    graph $graphName
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
                ?mapping a <http://graphBuilder.org/cachedMapping> .
                ?mapping <http://graphBuilder.org/hasMondoTerm> ?target .
                ?mapping <http://graphBuilder.org/hasIcdTerm> ?code .
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
        resultList.toArray
    }

    def getDiagnosisCodes(start: String, filterMethod: String, cxn: RepositoryConnection): Array[HashMap[String,String]] =
    {
        var graphName = "pmbb:cached_mondo_icd_mappings"
        if (filterMethod == "LEAF") graphName += "_LEAFONLY"
        else if (filterMethod == "REVERSEMAPS_DEPTH_FORMULA") graphName += "_depthTimesMappingCountFormulaOnly"

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
            graph $graphName
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
        PREFIX obo: <http://purl.obolibrary.org/obo/>
        PREFIX rxnorm: <http://purl.bioontology.org/ontology/RXNORM/>
        PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        select 
        distinct ?start ?source_full_name
        where {
            values ?start {
                $startListAsString
            }
            {
                values ?role_employment {
                    mydata:curated_role
                }
                values ?role_definer {
                    obo:chebi.owl
                }
                graph mydata:employment {
                    ?start mydata:employment ?role_employment .
                }
                graph mydata:defined_in {
                    ?start mydata:defined_in ?role_definer .
                }
                graph mydata:transitive_role_of_class {
                    ?solr_mediri mydata:transitive_role_of_class ?start .
                }
                values ?source_employment {
                    mydata:active_ingredient
                }
                values ?mapped_rxn_employment {
                    <http://example.com/resource/rxn_tty/IN>
                }
                values ?source_definer {
                    obo:chebi.owl
                }
                graph mydata:employment {
                    ?solr_mediri mydata:employment ?source_employment .
                }
                graph mydata:defined_in {
                    ?solr_mediri mydata:defined_in ?source_definer .
                }
                # for ChEBI/DrOn ingredient to rxnorm ingredient:
                #   union together direct BioPortal mappings
                #   with assertions from DrOn
                {
                    {
                        graph mydata:bioportal_mapping {
                            ?solr_mediri mydata:bioportal_mapping ?mapped_rxn
                        }
                    } union {
                        graph ?g {
                            ?dronprod mydata:transitively_materialized_dron_ingredient ?solr_mediri
                        }
                        # add triples patterns about the ?dronprod's definer and employment?
                        graph mydata:bioportal_mapping {
                            ?dronprod mydata:bioportal_mapping ?mapped_rxn
                        }
                    }
                }
                graph mydata:employment {
                    ?mapped_rxn mydata:employment ?mapped_rxn_employment .
                }
                graph mydata:defined_in {
                    ?mapped_rxn mydata:defined_in rxnorm: .
                }
                graph rxnorm: {
                    ?mapped_rxn  (rxnorm:constitutes|rxnorm:contained_in|rxnorm:has_dose_form|rxnorm:has_doseformgroup|rxnorm:has_form|rxnorm:has_tradename|rxnorm:ingredient_of|rxnorm:ingredients_of|rxnorm:isa|rxnorm:part_of|rxnorm:precise_ingredient_of|rxnorm:quantified_form_of|rxnorm:reformulation_of)* ?rxnprod .
                }
                # find orders for the bare ingredient or products containing the ingredient
                # probably won't find orders for a bare brand name
                {
                    {
                        graph mydata:elected_mapping {
                            ?order mydata:elected_mapping ?mapped_rxn .
                        }
                    } union {
                        graph mydata:elected_mapping {
                            ?order mydata:elected_mapping ?rxnprod .
                        }
                    }
                }
                # get properties of the order
                graph mydata:reference_medications {
                    ?order a obo:PDRO_0000024 ;
                           mydata:source_full_name ?source_full_name .
                }
                graph mydata:source_med_id  {
                    ?order skos:notation ?order_id .
                }
            }
            UNION
            {
                # the next two constraints can be applied in the SPARQL query, or could be ensured by some upstream logic
                values ?rxemployment {
                    <http://example.com/resource/rxn_tty/IN>
                }
                graph mydata:employment {
                    ?start mydata:employment ?rxemployment .
                }
                values ?source_definer {
                    rxnorm:
                }
                graph mydata:defined_in {
                    ?start mydata:defined_in ?source_definer .
                }
                # this is the "away from ingredient" RxNorm predicate whitelist
                graph rxnorm: {
                    ?start  (rxnorm:constitutes|rxnorm:contained_in|rxnorm:has_dose_form|rxnorm:has_doseformgroup|rxnorm:has_form|rxnorm:has_tradename|rxnorm:ingredient_of|rxnorm:ingredients_of|rxnorm:isa|rxnorm:part_of|rxnorm:precise_ingredient_of|rxnorm:quantified_form_of|rxnorm:reformulation_of)* ?rxnprod .
                }
                # find orders for the bare ingredient or products containing the ingredient
                # probably won't find orders for a bare brand name
                {
                    {
                        graph mydata:elected_mapping {
                            ?order mydata:elected_mapping ?start .
                        }
                    } union {
                        graph mydata:elected_mapping {
                            ?order mydata:elected_mapping ?rxnprod .
                        }
                    }
                }
                # get properties of the order
                graph mydata:reference_medications {
                    ?order a obo:PDRO_0000024 ;
                           mydata:source_full_name ?source_full_name .
                }
                graph mydata:source_med_id  {
                    ?order skos:notation ?order_id .
                }
            }
        }
      """
      //println(query)
      val tupleQueryResult = cxn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()
      val resultMap = new HashMap[String, ArrayBuffer[String]]
      while (tupleQueryResult.hasNext()) 
      {
          val bindingset: BindingSet = tupleQueryResult.next()
          var fullName: String = bindingset.getValue("source_full_name").toString
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
        logger.info("result size: " + buffResults.size)
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
          logger.info("result size: " + buffResults.size)
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