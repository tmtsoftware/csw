#!/usr/bin/env bash

pname=$(basename $0)

if [[ $# != 1 ]]; then
    echo "[ERROR] Argument missing! Results path not provided."
    exit 1
fi

topResultsPath=$1

currentPid=$$

existing_pid=`pgrep -f "top.sh" | grep -v ${currentPid}`
exist=$?

if [[ ${exist} != "0" ]]; then
    touch ${topResultsPath}
    echo "Running top command with interval of 1 second ..."
    echo "Saving Results of top in [$topResultsPath]"
    while true
    do
        top | grep -m10 "" >> ${topResultsPath}
        sleep 1
    done
else
    echo "=================================================================="
    echo "[WARN] Top is already running on this node with pid=[$existing_pid]."
    echo "=================================================================="
fi
