package nz.johniel

import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions

/**
 * @author Johniel Bocacao (300490028)
 */
object CICIDSClassifier extends App {
  val session = SparkSession
    .builder()
    .appName("CICIDSClassifier")
    .getOrCreate()

  val conf = session.sparkContext.hadoopConfiguration
  val fs = FileSystem.get(conf)
  val status = fs.listStatus(new Path("input"))
  val dataCount = status
    .map(x => x.getPath.toString)
    .count(_.contains("Friday-WorkingHours"))

  if (dataCount == 3) {
    val friday1 = session.read.options(Map("header"->"true", "inferSchema"->"true")).csv("input/Friday-WorkingHours-Morning.pcap_ISCX.csv")
    val friday2 = session.read.options(Map("header"->"true", "inferSchema"->"true")).csv("input/Friday-WorkingHours-Afternoon-DDos.pcap_ISCX.csv")
    val friday3 = session.read.options(Map("header"->"true", "inferSchema"->"true")).csv("input/Friday-WorkingHours-Afternoon-PortScan.pcap_ISCX.csv")
    val friday = friday1
      // Join separated files together
      .union(friday2)
      .union(friday3)
      // Reclassify
      .withColumn("class", functions.when(functions.col(" Label") === "DDoS", 1)
        .when(functions.col(" Label") === "BENIGN", 0)
        .otherwise(null))
    friday.printSchema()
    friday.show(2)

    println("First SparkContext:")
    println("APP Name :" + session.sparkContext.appName)
    println("Deploy Mode :" + session.sparkContext.deployMode)
    println("Master :" + session.sparkContext.master)
  } else {
    println("ERROR: 3 input/Friday-WorkingHours-*.csv's not found in HDFS")
  }
}
