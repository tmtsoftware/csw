#!/usr/bin/env bash

pname=$(basename $0)

if [[ $# != 1 ]]; then
    echo "[ERROR] Argument missing! Results path not provided."
    exit 1
fi

os=$(uname)
topResultsPath=$1
currentPid=$$

pgrep -f "top.sh" | grep -v ${currentPid}
exist=$?

if [[ ${exist} != "0" ]]; then
    touch ${topResultsPath}
    echo "Running top command with interval of 1 second ..."
    echo "Saving Results of top in [$topResultsPath]"

    if [[ ${os} == "Darwin" ]]; then
        while true
        do
            top | grep -m10 "" >> ${topResultsPath}
            sleep 1
        done
    else
        while true
        do
            top -b -n 1 | head -n 5 >> ${topResultsPath}
            sleep 1
        done
    fi
else
    echo "==========================================="
    echo "[WARN] Top is already running on this node."
    echo "==========================================="
fi
