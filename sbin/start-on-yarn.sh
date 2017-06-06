#!/usr/bin/env bash

function usage {
    echo "usage: -c <config file> -n <number Glint Instance>"
}

CONFIG_FILE=""
INSTANCE_NUMBER=0

while getopts "h?:n:v:c:" arg
do
    case ${arg} in
        h) usage;;
        c) CONFIG_FILE=${OPTARG};;
        n) INSTANCE_NUMBER=${OPTARG};;
        \?) usage
    esac
done

if [ ${INSTANCE_NUMBER} -lt 2 ]; then
    echo "Glint Instance Number should be more than 2"
    exit 1
fi

CONFIG_OPTION = ""
if [ "$CONFIG_FILE" != "" ]; then
    CONFIG_OPTION = "-c ${CONFIG_FILE}"
fi

# Set script path and load configuration
GLINT_SBIN_PATH=$(dirname "$0")
GLINT_SBIN_PATH=`cd $(dirname ${BASH_SOURCE[0]}); pwd`
source $GLINT_SBIN_PATH/configuration.sh

# Check Hadoop Conf Dir
if [ "$HADOOP_HOME" == "" ]; then
    echo "Without HADOOP_HOME environment var"
    exit -1
fi
HADOOP_BIN="$HADOOP_HOME/bin"
HADOOP_CMD="$HADOOP_BIN/hadoop"

# Upload Jar to HDFS
HADOOP_JAR_PATH="/tmp/$GLINT_JAR_NAME"
${HADOOP_CMD} fs -rm ${HADOOP_JAR_PATH}
${HADOOP_CMD} fs -put ${GLINT_JAR_PATH} ${HADOOP_JAR_PATH}

# Start on Yarn
${HADOOP_CMD} jar ${GLINT_JAR_PATH} glint.yarn.AppClient ${CONFIG_OPTION} -n ${INSTANCE_NUMBER} --path ${HADOOP_JAR_PATH}
${HADOOP_CMD} fs -rm ${HADOOP_JAR_PATH}
