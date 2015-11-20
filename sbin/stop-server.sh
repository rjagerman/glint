#!/usr/bin/env bash

# Set script path and load configuration
GLINT_SBIN_PATH=$(dirname "$0")
GLINT_SBIN_PATH=`cd $(dirname ${BASH_SOURCE[0]}); pwd`
source $GLINT_SBIN_PATH/configuration.sh

# Stop server
if [ -f "$GLINT_PATH/pids/server.pid" ]
then
    echo "Stopping server(s)"
    while read pid; do
        kill -SIGTERM $pid
    done <$GLINT_PATH/pids/server.pid
    rm $GLINT_PATH/pids/server.pid
else
    echo "No server process found"
fi
