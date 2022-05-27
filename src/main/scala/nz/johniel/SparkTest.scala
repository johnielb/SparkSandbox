package nz.johniel

import org.apache.spark.sql.SparkSession

/**
 * @author Johniel Bocacao (300490028)
 */
object SparkTest extends App {
  val spark = SparkSession.builder()
    .master("local[8]")
    .appName("SparkTest")
    .getOrCreate()

  println("First SparkContext:")
  println("APP Name :" + spark.sparkContext.appName)
  println("Deploy Mode :" + spark.sparkContext.deployMode)
  println("Master :" + spark.sparkContext.master)
}
