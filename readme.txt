# 1. Preparing program to run
- Download data from Kaggle: https://www.kaggle.com/code/kooaslansefat/cicids2017-safeml/data
- Extract all Friday-*.csv files to the data/ directory
- Setup dependencies
    - Obtain JARs from https://ecs.wgtn.ac.nz/foswiki/pub/Courses/AIML427_2022T1/Assignments/spark_jars.zip and extract to the lib/ directory

# 2. Run
java -cp "CICIDSClassfier.jar:lib/*" nz.johniel.CICIDSClassifier