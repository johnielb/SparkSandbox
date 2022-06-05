package nz.johniel

import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.evaluation.{BinaryClassificationEvaluator, RegressionEvaluator}
import org.apache.spark.ml.{Pipeline, PipelineStage}
import org.apache.spark.ml.feature.{StringIndexer, VectorAssembler}
import org.apache.spark.ml.tuning.{ParamGridBuilder, TrainValidationSplit}
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
    .master("local[8]")
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
  // Check how many Friday*.csv's are in the dir
  val status = fs.listStatus(new Path(path))
  val dataCount = status
    .map(x => x.getPath.toString)
    .count(_.contains("Friday-WorkingHours"))

  if (dataCount == 3) {
    // BEGIN INGESTING AND WRANGLING DATA ====================
    // 0. Load data
    val friday = readCSV(path + "Friday-WorkingHours-Morning.pcap_ISCX.csv") // Join separated files together
      .union(readCSV(path + "Friday-WorkingHours-Afternoon-DDos.pcap_ISCX.csv"))
      .union(readCSV(path + "Friday-WorkingHours-Afternoon-PortScan.pcap_ISCX.csv"))
    println("Raw data nrows: " + friday.count())
    // Drop NAs
    val fridayClean = friday.na.drop()
    println("Cleaned data nrows: " + fridayClean.count())

    // 1. Split test set off first thing
    val Array(train, test) = fridayClean.randomSplit(Array(0.7, 0.3), seed = 0)

    // 2. Define and build pipeline
    val featureCols = train.columns.filter(x => x != " Label")
    featureCols.mkString(",")
    val assembler = new VectorAssembler()
      .setInputCols(featureCols)
      .setOutputCol("features")
    val indexer = new StringIndexer()
      .setInputCol(" Label")
      .setOutputCol("label")
    val lr = new LogisticRegression()
      .setMaxIter(20)
      .setRegParam(0.01)
      .setElasticNetParam(0.1)
    val pipeline = new Pipeline()
      .setStages(Array(assembler, indexer, lr))
    val model = pipeline.fit(train)

    // 3. Test pipeline
    val predictions = model.transform(test)

    val evaluator = new BinaryClassificationEvaluator()
      .setLabelCol("label")
      .setRawPredictionCol("prediction")
      .setMetricName("areaUnderROC")
    println("Accuracy (AU-ROC): " + evaluator.evaluate(predictions))
//
//    // 3. Train pipeline with cross validation
//    val grid = new ParamGridBuilder()
//      .addGrid(lr.fitIntercept)
//      .addGrid(lr.regParam, Array(0.0001, 0.001, 0.01, 0.1, 0.5, 1))
//      .addGrid(lr.elasticNetParam, Array(0, 1, 0.01))
//      .build()
//    val cv = new TrainValidationSplit()
//      .setEstimator(pipeline)
//      .setEvaluator(new BinaryClassificationEvaluator())
//      .setEstimatorParamMaps(grid)
//      .setTrainRatio(0.3)
//    val model = cv.fit(train)
//
//    // 4. Test pipeline
//    model.transform(test)
//      .select("features", "label", "prediction")
//      .show()
  } else {
    println("ERROR: 3 input/Friday-WorkingHours-*.csv's not found in data directory or HDFS input")
  }

  private def readCSV(filePath: String) = {
    session.read
      .options(Map("header" -> "true", "inferSchema" -> "true"))
      .csv(filePath)
  }
}
