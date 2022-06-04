# 1. Preparing program to run
- Download data from Kaggle: https://www.kaggle.com/code/kooaslansefat/cicids2017-safeml/data
- Extract all Friday-*.csv files to a working directory before putting on hdfs using
```
hdfs dfs -put Friday-WorkingHours-Morning.pcap_ISCX.csv Friday-WorkingHours-Afternoon-PortScan.pcap_ISCX.csv Friday-WorkingHours-Afternoon-DDos.pcap_ISCX.csv input
```
- Setup dependencies
    - Download Spark 2.4.2 for Hadoop 2.7 from https://archive.apache.org/dist/spark/spark-2.4.2/
    - Extract spark-2.4.2-bin-hadoop2.7.tgz to an accessible directory using
    ```
    tar -zvf spark-2.4.2-bin-hadoop2.7.tgz
    ```
    - Obtain JARs from https://ecs.wgtn.ac.nz/foswiki/pub/Courses/AIML427_2022T1/Assignments/spark_jars.zip
    - Extract JARs to <spark-2.4.2-bin-hadoop2.7 directory>/jars
    - Add Spark and Hadoop directories to the class path, and set up Hadoop environment variables by sourcing this script
    ```
    source SetupSparkClasspath.sh
    ```

# 2. Run
To deploy the classifier in a local client (with n workers, n <= MAX_THREADS):
```
spark-submit --class "nz.johniel.CICIDSClassifier" --master "local[n]" CICIDSClassifier.jar
```
To deploy the classifier in a cluster:
```
spark-submit --class "nz.johniel.CICIDSClassifier" --master yarn --deploy-mode cluster CICIDSClassifier.jar
```
