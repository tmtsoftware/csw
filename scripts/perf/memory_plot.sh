#!/usr/bin/env bash

# Before running this script, make sure that you have install gnuplot with qt and cairo
# Installation:
#   Mac:                    brew install gnuplot --with-cairo
#   Ubuntu:                 sudo apt-get install libcairo2-dev
#                           sudo apt-get install libpango1.0-dev
#                           sudo apt-get install gnuplot
#   Fedora/CENTOS/Redhat:   sudo yum install pango-devel
#                           sudo yum install gnuplot

if [[ $# != 1 ]]; then
    echo "[ERROR] Argument missing!"
    echo "[ERROR] This script expect file path which contains results captured from running top command."
    exit 1
else
    if [[ ! -f $1 ]]; then
        echo "[ERROR] $1 is either directory or not a valid file path."
        exit 1
    fi
fi

topResultsPath=$1

timestamp=`date +%F_%H-%M-%S`

memoryUsagePath="$HOME/perf/memory_usage_$timestamp.log"
tmpFile="$HOME/perf/memory_usage_$timestamp.tmp"
memoryUsageGraphPath="$HOME/perf/memory_usage_plot_$timestamp.png"

if [ ! -f ${topResultsPath} ]; then
  exit 1
fi

os=`uname`

echo "Extracting Memory usage from [$topResultsPath]"

if [[ ${os} == "Darwin" ]]; then
    `awk '/PhysMem/ {printf ("%s\\t%s\\n", i++, $2)}' ${topResultsPath} >> ${memoryUsagePath}`

    awk 'BEGIN{ FS=OFS="\t" }{ s=substr($2,1,length($2)); u=substr($2,length($2));
         if(u=="K") $2=(s/1024)"M"; else if(u=="G") $2=(s*1024)"M" }1' ${memoryUsagePath} > ${tmpFile} && mv ${tmpFile} ${memoryUsagePath}
else
    `awk '/KiB Mem/ {printf ("%s\\t%s\\n", i++, $8/1024)}' ${topResultsPath} >> ${memoryUsagePath}`
fi


echo "Adding headers in [$memoryUsagePath]"
sed -i 1i"Seconds\\tMemory(MB)" ${memoryUsagePath}

echo "============================================================================="
echo "Plotting Memory usage graph using gnuplot at [$memoryUsageGraphPath]"
echo "============================================================================="

gnuplot <<-EOFMarker
    set xlabel "Seconds"
    set ylabel "Memory Usage (MB)"
    set title "Memory Usage"
    set term pngcairo size 1680,1050 enhanced font 'Verdana,18'
    set output "${memoryUsageGraphPath}"
    plot "${memoryUsagePath}" using 1:2 with lines title columnheader
EOFMarker
