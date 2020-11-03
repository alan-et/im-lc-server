#!/bin/sh

CUR_DIR=$(cd $(dirname $0);pwd)
cd ${CUR_DIR}

source ./common.sh

PID=$(get_process_id)

if [ -z "${PID}" ]; then
    echo "${APP_NAME} process is not exists"
    exit 0
fi

trycount=0
echo "start stop ${APP_NAME}"
IS_STOPED="false"
while [ $trycount -lt 20 ]; do
    kill ${PID}
    sleep 0.5

    IS_EXISTS=$(is_process_exists)
    if [ "false" = "${IS_EXISTS}" ]; then
        echo "${APP_NAME}(${PID}) stoped"
        rm ${PID_FILE}
        IS_STOPED="true"
        break
    fi

    trycount=$(expr $trycount + 1)
done

if [ "true" = "${IS_STOPED}" ]; then
    exit 0
fi

echo "Can't stop ${APP_NAME}(${PID}) gracefully.It will be force stoped"
kill -9 ${PID}
rm ${PID_FILE}


