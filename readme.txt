# 1. Preparing program to run
1. Setup dependencies by adding Spark and Hadoop directories to the path EV, and set up Hadoop EVs by sourcing this script:
    `source SetupSparkClasspath.sh`
2. Download the 3 Friday-*.csv files from Kaggle:
    https://www.kaggle.com/code/kooaslansefat/cicids2017-safeml/data
3. Extract all Friday-*.csv files to a working directory before putting on hdfs using:
   `hdfs dfs -put Friday-WorkingHours-Morning.pcap_ISCX.csv Friday-WorkingHours-Afternoon-PortScan.pcap_ISCX.csv Friday-WorkingHours-Afternoon-DDos.pcap_ISCX.csv input`
4. Download CICIDSClassifier.jar to the working directory.

# 2. Run
To deploy the classifier in a local client (with n workers, 0 < n <= MAX_THREADS):
`spark-submit --class "nz.johniel.CICIDSClassifier" --master "local[n]" CICIDSClassifier.jar local`

To deploy the classifier in a cluster:
`spark-submit --class "nz.johniel.CICIDSClassifier" --master yarn --deploy-mode cluster CICIDSClassifier.jar hdfs`

To add a PCA stage to the pipeline, append "pca" to the command.
To add a normalisation stage to the pipeline, append "norm" to the command.
