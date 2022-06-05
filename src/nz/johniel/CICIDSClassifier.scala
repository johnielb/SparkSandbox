package nz.johniel

import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions

/**
 * @author Johniel Bocacao (300490028)
 */
object CICIDSClassifier extends App {
  if (args.length != 1) {
    println("usage: spark-submit --class \"nz.johniel.CICIDSClassifier\" --master \"local[n]\" CICIDSClassifier.jar (hdfs | local)")
    System.exit(1)
  }
  val session = SparkSession
    .builder()
//    .master("local[8]")
    .appName("CICIDSClassifier")
    .getOrCreate()

  println("================ Spark Session ================")
  println("App Name: " + session.sparkContext.appName)
  println("Deploy Mode: " + session.sparkContext.deployMode)
  println("Manager: " + session.sparkContext.master)

  val loadType = args(0)
  val conf = session.sparkContext.hadoopConfiguration
  var fs : FileSystem = _
  var path : String = _
  if (loadType == "hdfs") {
    fs = FileSystem.get(conf)
    path = "input/"
  } else if (loadType == "local") {
    fs = FileSystem.getLocal(conf)
    path = "data/"
  } else {
    println("usage: spark-submit --class \"nz.johniel.CICIDSClassifier\" --master \"local[n]\" CICIDSClassifier.jar (hdfs | local)")
    System.exit(1)
  }

  val status = fs.listStatus(new Path(path))
  val dataCount = status
    .map(x => x.getPath.toString)
    .count(_.contains("Friday-WorkingHours"))

  if (dataCount == 3) {
    val friday = readCSV(path + "Friday-WorkingHours-Morning.pcap_ISCX.csv") // Join separated files together
      .union(readCSV(path + "Friday-WorkingHours-Afternoon-DDos.pcap_ISCX.csv"))
      .union(readCSV(path + "Friday-WorkingHours-Afternoon-PortScan.pcap_ISCX.csv"))
    friday.printSchema()
    friday.show(2)
  } else {
    println("ERROR: 3 input/Friday-WorkingHours-*.csv's not found in data directory or HDFS input")
  }

  private def readCSV(filePath: String) = {
    session.read
      .options(Map("header" -> "true", "inferSchema" -> "true"))
      .csv(filePath)
  }
}
