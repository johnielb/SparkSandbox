package nz.johniel

import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.ml.classification.DecisionTreeClassifier
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.ml.feature.{StringIndexer, VectorAssembler}
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.sql.{DataFrame, SparkSession}

/**
 * @author Johniel Bocacao (300490028)
 */
object CICIDSClassifier extends App {
  private val usage = "usage: spark-submit --class \"nz.johniel.CICIDSClassifier\" --master (\"local[n]\" | yarn) CICIDSClassifier.jar (hdfs | local)"
  if (args.length != 1) {
    println(usage)
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
  var path = "input/"
  val verbose = true
  if (loadType == "hdfs") {
    fs = FileSystem.get(conf)
  } else if (loadType == "local") {
    fs = FileSystem.getLocal(conf)
  } else {
    println(usage)
    System.exit(1)
  }
  // Count how many Friday*.csv's are in the dir
  val dataCount = fs.listStatus(new Path(path))
    .map(x => x.getPath.toString)
    .count(_.contains("Friday-WorkingHours"))

  if (dataCount == 3) {
    // BEGIN INGESTING AND WRANGLING DATA ====================
    // 0. Load data
    val friday = readCSV(path + "Friday-WorkingHours-Morning.pcap_ISCX.csv") // Join separated files together
      .union(readCSV(path + "Friday-WorkingHours-Afternoon-DDos.pcap_ISCX.csv"))
      .union(readCSV(path + "Friday-WorkingHours-Afternoon-PortScan.pcap_ISCX.csv"))
    // Drop NAs
    val fridayClean = friday.na.drop()
    if (verbose) {
      println("Raw data nrows: " + friday.count())
      println("Cleaned data nrows: " + fridayClean.count())
    }

    // 1. Split test set off first thing ============================================
    // Stratify sampling by label, get 50% from each class
    val train = fridayClean.stat.sampleBy(" Label", Map("BENIGN" -> 0.5, "PortScan" -> 0.5, "Bot" -> 0.5, "DDoS" -> 0.5), 0)
    // Get the test set by getting the set exception
    val test = fridayClean.exceptAll(train)
    if (verbose) {
      train.groupBy(" Label").count().show()
      println("Train nrows: " + train.count())
      test.groupBy(" Label").count().show()
      println("Test nrows: " + test.count())
    }

    // 2. Define and build pipeline ============================================
    val featureCols = train.columns.filter(x => x != " Label")
    if (verbose) println("Features: " + featureCols.mkString(", "))

    val pipeline = new Pipeline()
      .setStages(Array(
        // Group features into a vector
        new VectorAssembler()
          .setInputCols(featureCols)
          .setOutputCol("features"),
        // Index label strings as integers
        new StringIndexer()
          .setInputCol(" Label")
          .setOutputCol("label"),
        // Classifier as final step
        new DecisionTreeClassifier()
      ))
    val model = pipeline.fit(train)

    // 3. Test pipeline on train data ============================================
    println("> Training metrics (" + train.count() + " instances)")
    testPipeline(model, train)

    // 4. Test pipeline on test data ============================================
    println("> Test metrics(" + test.count() + " instances)")
    testPipeline(model, test)
  } else {
    println("ERROR: 3 input/Friday-WorkingHours-*.csv's not found in data directory or HDFS input")
  }

  private def testPipeline(model : PipelineModel, df : DataFrame): Unit = {
    val predictions = model.transform(df)
    val evaluator = new MulticlassClassificationEvaluator()
      .setPredictionCol("prediction")
      .setMetricName("accuracy")
    val accuracy = evaluator.evaluate(predictions)
    println("Accuracy = " + accuracy)
  }

  private def readCSV(filePath: String) = {
    session.read
      .options(Map("header" -> "true", "inferSchema" -> "true"))
      .csv(filePath)
  }
}
