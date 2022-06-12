package nz.johniel

import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.ml.classification.{DecisionTreeClassificationModel, DecisionTreeClassifier}
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.ml.feature.{PCA, StandardScaler, StringIndexer, VectorAssembler}
import org.apache.spark.ml.{Pipeline, PipelineModel, PipelineStage}
import org.apache.spark.sql.functions.{col, countDistinct}
import org.apache.spark.sql.{DataFrame, SparkSession}

/**
 * @author Johniel Bocacao (300490028)
 */
object CICIDSClassifier extends App {
  private val usage = "usage: spark-submit --class \"nz.johniel.CICIDSClassifier\" --master (\"local[n]\" | yarn) CICIDSClassifier.jar (hdfs | local) (norm) (pca)"
  if (args.length < 1) {
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
  val hadoopConf = session.sparkContext.hadoopConfiguration
  var fs : FileSystem = _
  var path = "input/"
  val verbose = true
  if (loadType == "hdfs") {
    fs = FileSystem.get(hadoopConf)
  } else if (loadType == "local") {
    fs = FileSystem.getLocal(hadoopConf)
  } else {
    println(usage)
    System.exit(1)
  }
  // Count how many Friday*.csv's are in the dir
  val dataCount = fs.listStatus(new Path(path))
    .map(x => x.getPath.getName)
    .count(_.startsWith("Friday-WorkingHours"))

  if (dataCount == 3) {
    // BEGIN INGESTING AND WRANGLING DATA ====================
    // 0. Load data
    // Join separated files together
    val friday = readCSV(path + "Friday-WorkingHours-Morning.pcap_ISCX.csv")
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
    val train = fridayClean.stat.sampleBy(" Label",
      Map("BENIGN" -> 0.5, "PortScan" -> 0.5, "Bot" -> 0.5, "DDoS" -> 0.5), 0)
    // Get the test set by getting the set exception
    val test = fridayClean.exceptAll(train)
    if (verbose) {
      train.groupBy(" Label").count().show()
      println("Train nrows: " + train.count())
      test.groupBy(" Label").count().show()
      println("Test nrows: " + test.count())
    }

    // 2. Define and build pipeline ============================================
    var featureCols = train.columns.filter(x => x != " Label")
    // If PCA, remove zero-variance features
    if (args.contains("pca")) {
      val countMap = (train.columns zip train.select(
        train.columns.map(c => countDistinct(col(c)).alias(c)): _*
      ).collect().head.toSeq)
        .toMap
      // Zip column name as key, count of distinct values as value
      val zeroVarianceCols = countMap
        // Only keep if they have 1 value - constant, no variance
        .filter(x => x._2 == 1)
        .keys
        .toList
      // Keep features that don't have zero variance
      featureCols = featureCols.filter(x => !zeroVarianceCols.contains(x) &&
        // Problematic columns removed which I don't have time to figure out why
        x != "Flow Bytes/s" && x != " Flow Packets/s")
    }
    var featuresCol = "features"
    if (verbose) println(featureCols.length + " features:" + featureCols.mkString(","))

    var stages = Array[PipelineStage](
      // Group features into a vector
      new VectorAssembler()
        .setInputCols(featureCols)
        .setOutputCol(featuresCol),
      // Index label strings as doubles
      new StringIndexer()
        .setInputCol(" Label")
        .setOutputCol("label")
    )
    if (args.contains("norm")) {
      featuresCol = "scaledFeatures"
      stages :+= new StandardScaler()
        .setWithMean(true)
        .setInputCol("features")
        .setOutputCol(featuresCol)
    }
    if (args.contains("pca")) {
      featuresCol = "pcaFeatures"
      stages :+= new PCA()
        .setInputCol("features")
        .setOutputCol(featuresCol)
        .setK(20)
    }

    // Classifier as final step
    stages :+= new DecisionTreeClassifier()
      .setFeaturesCol(featuresCol)
    val pipeline = new Pipeline()
      .setStages(stages)
    if (verbose) println(stages.mkString(" %>% "))
    val model = pipeline.fit(train)
    if (verbose) println(model.stages.last.asInstanceOf[DecisionTreeClassificationModel].toDebugString)

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
