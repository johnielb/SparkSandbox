# 1. Preparing program to run
1. Setup dependencies
    a. Download Spark 2.4.2 for Hadoop 2.7 from https://archive.apache.org/dist/spark/spark-2.4.2/
    b. Extract spark-2.4.2-bin-hadoop2.7.tgz to an accessible directory using
    ```
    tar -zvf spark-2.4.2-bin-hadoop2.7.tgz
    ```
    c. Obtain JARs from https://ecs.wgtn.ac.nz/foswiki/pub/Courses/AIML427_2022T1/Assignments/spark_jars.zip
    d. Extract JARs to <spark-2.4.2-bin-hadoop2.7 directory>/jars
    e. Add Spark and Hadoop directories to the path EV, and set up Hadoop EVs by sourcing this script
    ```
    source SetupSparkClasspath.sh
    ```
2. Download data from Kaggle: https://www.kaggle.com/code/kooaslansefat/cicids2017-safeml/data
3. Extract all Friday-*.csv files to a working directory before putting on hdfs using
```
hdfs dfs -put Friday-WorkingHours-Morning.pcap_ISCX.csv Friday-WorkingHours-Afternoon-PortScan.pcap_ISCX.csv Friday-WorkingHours-Afternoon-DDos.pcap_ISCX.csv input
```

# 2. Run
To deploy the classifier in a local client (with n workers, 0 < n <= MAX_THREADS):
```
spark-submit --class "nz.johniel.CICIDSClassifier" --master "local[8]" CICIDSClassifier.jar local
```
To deploy the classifier in a cluster:
```
spark-submit --class "nz.johniel.CICIDSClassifier" --master yarn --deploy-mode cluster CICIDSClassifier.jar hdfs
```
