MAIN_CLASS=CICIDSClassifier
COMPILATION_OUT=out/production
JAR=${MAIN_CLASS}.jar
ECS_PROJ_DIR=AIML427/Spark/${MAIN_CLASS}

scalac -classpath "lib/*" -d ${COMPILATION_OUT} src/nz/johniel/${MAIN_CLASS}.scala
jar cmvf src/META-INF/MANIFEST.MF ${JAR} -C ${COMPILATION_OUT} .
sftp bocacajohn@barretts.ecs.vuw.ac.nz:${ECS_PROJ_DIR} <<< "put ${JAR}"