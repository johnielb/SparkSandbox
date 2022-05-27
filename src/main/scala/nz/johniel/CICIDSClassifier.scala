package nz.johniel

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.when
import org.apache.spark.sql.functions.col

/**
 * @author Johniel Bocacao (300490028)
 */
object CICIDSClassifier extends App {
  val session = SparkSession.builder()
    .master("local[8]")
    .appName("CICIDSClassifier")
    .getOrCreate()
  val friday1 = session.read.options(Map("header"->"true", "inferSchema"->"true")).csv("data/Friday-WorkingHours-Morning.pcap_ISCX.csv")
  val friday2 = session.read.options(Map("header"->"true", "inferSchema"->"true")).csv("data/Friday-WorkingHours-Afternoon-DDos.pcap_ISCX.csv")
  val friday3 = session.read.options(Map("header"->"true", "inferSchema"->"true")).csv("data/Friday-WorkingHours-Afternoon-PortScan.pcap_ISCX.csv")
  val friday = friday1
    // Join separated files together
    .union(friday2)
    .union(friday3)
    // Reclassify
    .withColumn("class", when(col(" Label") === "DDoS", 1)
      .when(col(" Label") === "BENIGN", 0)
      .otherwise(null))
  friday.printSchema()
  friday.show(2)

  println("First SparkContext:")
  println("APP Name :" + session.sparkContext.appName)
  println("Deploy Mode :" + session.sparkContext.deployMode)
  println("Master :" + session.sparkContext.master)
}
