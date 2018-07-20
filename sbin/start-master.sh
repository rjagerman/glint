#!/usr/bin/env bash

# Set script path and load configuration
GLINT_SBIN_PATH=$(dirname "$0")
GLINT_SBIN_PATH=`cd $(dirname ${BASH_SOURCE[0]}); pwd`
source $GLINT_SBIN_PATH/configuration.sh

# Start master
#nohup java $GLINT_MASTER_OPTS -jar $GLINT_JAR_PATH master -c $GLINT_PATH/conf/default.conf > $GLINT_PATH/logs/master-out.log 2> $GLINT_PATH/logs/master-err.log &
#echo $! >> $GLINT_PATH/pids/master.pid
nohup java $GLINT_MASTER_OPTS -cp $GLINT_JAR_PATH glint.Main master -c $GLINT_PATH/conf/default.conf > $GLINT_PATH/logs/master-out.log 2> $GLINT_PATH/logs/master-err.log &
echo $! >> $GLINT_PATH/pids/master.pid
