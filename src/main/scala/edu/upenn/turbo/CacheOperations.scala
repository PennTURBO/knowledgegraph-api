package edu.upenn.turbo

import java.io.File
import org.eclipse.rdf4j.repository.Repository
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import java.nio.file.Path
import java.nio.file.Paths
import java.io.PrintWriter
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.WildcardFileFilter
import java.io.BufferedReader
import java.io.FileReader

import carnival.core.util.GenericDataTable
import collection.JavaConverters._

class CacheOperations
{

    var prefixLookup: HashMap[String, String] = new HashMap[String, String]
    prefixLookup += "diseaseToICD" -> "GRAPHDB"
    prefixLookup += "medToOrderName" -> "NEO4J"

    def checkIfCurrentCacheExists(filePrefix: String, directory: String = "cache"): Option[String] =
    {
        var cacheFile = None : Option[String]
        if(!prefixLookup.contains(filePrefix.split("_")(0))) throw new RuntimeException ("Provided file prefix " + filePrefix.split("_")(0) + " not recognized as an operation")
        var prefixForLookup = filePrefix.split("_")(0)
        val matchedFiles = getAllFilesWithMatchingPrefix(filePrefix, directory)
        if (matchedFiles.size > 1) throw new RuntimeException ("Multiple cache files found for prefix " + filePrefix)
        if (matchedFiles.size == 0) None
        else
        {
            if(!isCacheFileUpToDate(matchedFiles(0).toString, prefixLookup(prefixForLookup))) None
            else Some(matchedFiles(0).toString)
        }
    }

    def getResults(cacheFile: String): Array[String] =
    {
       var resArr = new ArrayBuffer[String]
       println("cache file: " + cacheFile)
       val splitFile = cacheFile.split("//")
       var localPath = splitFile(1)
       if (localPath.endsWith(".csv")) localPath = localPath.substring(0, localPath.length - 4)
       val cacheData = GenericDataTable.createFromFiles(new File(splitFile(0)), localPath).dataIterator
       while (cacheData.hasNext) resArr += cacheData.next.toString.split("=")(1).replaceAll("}", "")
       resArr.toArray
    }

    def writeResults(userQuery: String, results: Array[String], requestType: String)
    {
        val tableName = requestType + "_" + userQuery
        val dt = new GenericDataTable(Map("name" -> tableName).asJava)
        val map1 = new HashMap[String, Object]()
        for (a <- results) dt.dataAdd(Map(userQuery -> a.asInstanceOf[Object]).asJava)
        dt.writeFiles(new File("cache//"), Map("appendDateSuffix" -> true).asJava)
    }

    def getAllFilesWithMatchingPrefix(filePrefix: String, directory: String): Array[Object] =
    {
        val dir = new File(directory)
        var matchedFiles = FileUtils.listFiles(dir, new WildcardFileFilter(filePrefix + "*"), null).toArray
        var finalRes = new ArrayBuffer[Object]
        for (a <- matchedFiles) 
        {
            if (a.toString.endsWith(".csv")) finalRes += a
        }
        finalRes.toArray
    }

    def isCacheFileUpToDate(file: String, graph: String): Boolean =
    {
        /*var graphDate = None : Option[Date]
        if (graph == "GRAPHDB") graphDate = Some(getLatestGraphDBUpdate())
        if (graph == "NEO4J") graphDate = Some(getLatestNeo4JUpdate())*/

        //compare dates, return "true" if cache file creation is after last graph update, "false" if last graph update is after cache file creation
        true
    }
}