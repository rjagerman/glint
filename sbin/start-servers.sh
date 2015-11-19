#!/usr/bin/env bash

# Set script path and load configuration
GLINT_SBIN_PATH=$(dirname "$0")
GLINT_SBIN_PATH=`cd $(dirname ${BASH_SOURCE[0]}); pwd`
source $GLINT_SBIN_PATH/configuration.sh

# Start servers
if [ -f "$GLINT_PATH/conf/servers" ]
then
    echo "Starting remote servers"
    while read server; do {
        server=`echo $server | sed 's/\#.*//'`
        if [[ $server != "" ]]
        then
            echo "Starting server $server"
            ssh $server "$GLINT_SBIN_PATH/start-server.sh"
        fi
    } < /dev/null ;done <$GLINT_PATH/conf/servers
else
    echo "No servers file found in configuration"
fi
