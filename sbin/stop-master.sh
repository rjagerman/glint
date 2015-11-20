#!/usr/bin/env bash

# Set script path and load configuration
GLINT_SBIN_PATH=$(dirname "$0")
GLINT_SBIN_PATH=`cd $(dirname ${BASH_SOURCE[0]}); pwd`
source $GLINT_SBIN_PATH/configuration.sh

# Stop master
if [ -f "$GLINT_PATH/pids/master.pid" ]
then
    echo "Stopping master"
    while read pid; do
        kill -SIGTERM $pid
    done <$GLINT_PATH/pids/master.pid
    rm $GLINT_PATH/pids/master.pid
else
    echo "No master process found"
fi
