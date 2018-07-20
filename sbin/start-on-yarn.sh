#!/usr/bin/env bash
export HADOOP_HOME=/usr/hdp/current/hadoop-client
export YARN_HOME=$HADOOP_HOME

function usage {
    echo "usage: -c <config file> -n <number Glint Instance> -H <hdfs://hdfs-addresss:port>"
}

CONFIG_FILE=""
INSTANCE_NUMBER=0
HDFS_PREFIX=""

while getopts "h?:n:c:H:" arg
do
    case ${arg} in
        h) usage;;
        H) HDFS_PREFIX=${OPTARG};;
        c) CONFIG_FILE=${OPTARG};;
        n) INSTANCE_NUMBER=${OPTARG};;
        \?) usage
    esac
done

if [ ${INSTANCE_NUMBER} -lt 2 ]; then
    echo "Glint Instance Number should be more than 2"
    exit 1
fi

CONFIG_OPTION=""
if [ "$CONFIG_FILE" != "" ]; then
    CONFIG_OPTION="-c ${CONFIG_FILE}"
fi

if [ "$HDFS_PREFIX" == "" ]; then
    echo "Not Specific HDFS Prefix"
    exit 1
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

# Prepare Logs
mkdir -p $GLINT_PATH/pids
mkdir -p $GLINT_PATH/logs

# Upload Jar to HDFS
HADOOP_JAR_PATH="$HDFS_PREFIX/$GLINT_JAR_NAME"
${HADOOP_CMD} fs -rm ${HADOOP_JAR_PATH} > $GLINT_PATH/logs/glint-on-yarn-out.log 2> $GLINT_PATH/logs/glint-on-yarn-err.log
${HADOOP_CMD} fs -put ${GLINT_JAR_PATH} ${HADOOP_JAR_PATH} > $GLINT_PATH/logs/glint-on-yarn-out.log 2> $GLINT_PATH/logs/glint-on-yarn-err.log

# Start on Yarn
${HADOOP_CMD} jar ${GLINT_JAR_PATH} glint.yarn.AppClient ${CONFIG_OPTION} -n ${INSTANCE_NUMBER} --path ${HADOOP_JAR_PATH} > $GLINT_PATH/logs/glint-on-yarn-out.log 2> $GLINT_PATH/logs/glint-on-yarn-err.log &
