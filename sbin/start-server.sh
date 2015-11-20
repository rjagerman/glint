#!/usr/bin/env bash

# Set script path and load configuration
GLINT_SBIN_PATH=$(dirname "$0")
GLINT_SBIN_PATH=`cd $(dirname ${BASH_SOURCE[0]}); pwd`
source $GLINT_SBIN_PATH/configuration.sh

# Start server
mkdir -p $GLINT_PATH/pids
mkdir -p $GLINT_PATH/logs
nohup java $GLINT_SERVER_OPTS -jar $GLINT_JAR_PATH server -c $GLINT_PATH/conf/default.conf > $GLINT_PATH/logs/server-out.log 2> $GLINT_PATH/logs/server-err.log &
echo $! >> $GLINT_PATH/pids/server.pid
