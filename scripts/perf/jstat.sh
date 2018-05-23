#!/usr/bin/env bash

timestamp=`date +%F_%H-%M-%S`

pid=$1
name=$2

jstatResultsPath="$HOME/perf/jstat/${name}_jstat_${pid}_$timestamp.log"

mkdir -p $(dirname "$jstatResultsPath")

touch ${jstatResultsPath}

echo "Running jstat script: [$0]"
echo "Saving Results of jstat in $jstatResultsPath"

nohup jstat -gc -t ${pid} 1s >> ${jstatResultsPath} &
