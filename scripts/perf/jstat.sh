#!/usr/bin/env bash

timestamp=`date +%F_%H-%M-%S`

pid=$1
jstatResultsPath="$HOME/perf/jstat_${pid}_$timestamp.log"

touch ${jstatResultsPath}

echo "Running jstat script: [$0]"
echo "Saving Results of jstat in $jstatResultsPath"

nohup jstat -gc ${pid} 1s >> ${jstatResultsPath} &
