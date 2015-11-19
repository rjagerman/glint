#!/usr/bin/env bash

# Set script path and load configuration
GLINT_SBIN_PATH=$(dirname "$0")
GLINT_SBIN_PATH=`cd $(dirname ${BASH_SOURCE[0]}); pwd`
source $GLINT_SBIN_PATH/configuration.sh

# Start master
nohup java -jar $GLINT_PATH/bin/Glint-assembly-0.1-SNAPSHOT.jar master -c $GLINT_PATH/conf/default.conf > $GLINT_PATH/logs/master-out.log 2> $GLINT_PATH/logs/master-err.log &
echo $! >> $GLINT_PATH/pids/master.pid
