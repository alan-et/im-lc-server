#!/bin/sh

CUR_DIR=$(cd $(dirname $0);pwd)
cd ${CUR_DIR}

source /etc/profile > /dev/null 2>&1
source ./common.sh

PROC_EXISTS=$(is_process_exists)

if [ "true" = "${PROC_EXISTS}" ]; then
    echo "the ${APP_NAME} process is exists"
    exit 0
fi

DUBBO_PROPERTIES=${WS_DIR}/config/dubbo.properties

JAVA=java

jars="${WS_DIR}/build/libs/${APP_NAME}-0.0.1-SNAPSHOT.jar"

#JAVA_OPTS=" -Ddubbo.properties.file=${DUBBO_PROPERTIES}"
#JAVA_OPTS="$JAVA_OPTS -Dcsp.sentinel.log.dir=${WS_DIR}/logs/"
JAVA_OPTS="$JAVA_OPTS -Dproject.name=${APP_NAME}"
JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${LOGS_DIR}"
JAVA_OPTS="$JAVA_OPTS -XX:+PrintGCDetails -Xloggc:${LOGS_DIR}/gc.log"

nohup $JAVA -jar $JAVA_OPTS $jars >> ${STD_LOG_FILE} 2>&1 &
pid=$!

echo $pid > ${PID_FILE}

echo "${APP_NAME} start pid:$pid"




