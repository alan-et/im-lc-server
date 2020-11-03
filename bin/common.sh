#!/usr/bin/env bash

WS_DIR=$(cd ${CUR_DIR}/..;pwd)
cd ${WS_DIR}
echo "workspace dir:${WS_DIR}"

LOGS_DIR=${WS_DIR}/logs
if [ ! -d "${LOGS_DIR}" ]; then
    mkdir ${LOGS_DIR}
fi

#CONF=config/application.properties

#根据key得到指定val
function get_property(){
	profile=$1
	pkey=$2

	if [ -z "$pkey" ]; then
        	echo ""
        	exit 0
	fi

	#echo "read file"
	while read data; do
        	key=`echo $data|awk -F= '{ print $1 }'`
        	val=`echo $data|awk -F= '{ print $2 }'`
        	if [ "$key" = "$pkey" ]; then
                	echo "$val"
			break
        	fi
	done < $profile
}


#APP_NAME=$(get_property ${CONF} "app.name")
APP_NAME=$(echo ${WS_DIR} | awk -F\/ '{ print $NF }')
echo "APP_NAME: ${APP_NAME}"

PID_FILE="${LOGS_DIR}/${APP_NAME}.pid"
echo "pid file: ${PID_FILE}"

STD_LOG_FILE="${LOGS_DIR}/stdout.${APP_NAME}.log"
echo "std log file: ${STD_LOG_FILE}"


#得到进程的ID
function get_process_id(){
    if [ ! -f "${PID_FILE}" ]; then
        echo ""
        return
    fi
    PID=$(cat ${PID_FILE})
    echo "${PID}"
}

#检查进程是否存在
function is_process_exists(){
    PID=$(get_process_id)
    if [ -z "${PID}" ]; then
        echo "false"
        return
    fi

    PROC_INFO=$(ps -ef|grep -w ${PID}|grep -v grep)
    if [ -z "${PROC_INFO}" ]; then
        echo "false"
    else
        echo "true"
    fi
}



