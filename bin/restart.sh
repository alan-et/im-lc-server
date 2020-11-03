#!/usr/bin/env bash

CUR_DIR=$(cd $(dirname $0);pwd)
cd ${CUR_DIR}

./shutdown.sh
./start.sh