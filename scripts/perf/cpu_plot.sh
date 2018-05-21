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

cpuUsagePath="$HOME/perf/cpu_usage_$timestamp.log"
cpuUsageGraphPath="$HOME/perf/cpu_usage_plot_$timestamp.png"

if [ ! -f ${topResultsPath} ]; then
  exit 1
fi

os=`uname`

echo "Extracting CPU usage from [$topResultsPath]"
if [[ ${os} == "Darwin" ]]; then
    `awk '/CPU/ {printf ("%s\\t%s\\t%s\\t%s\\n", i++, $3, $5, $7)}' ${topResultsPath} >> ${cpuUsagePath}`
else
    `awk '/%Cpu(s)/ {printf ("%s\\t%s\\t%s\\t%s\\n", i++, $2, $4, $8)}' ${topResultsPath} >> ${cpuUsagePath}`
fi

echo "Adding headers in [$cpuUsagePath]"
sed -i 1i"seconds\\tUser\\tSystem\\tIdle" ${cpuUsagePath}

echo "============================================================================="
echo "Plotting CPU usage graph using gnuplot at [$cpuUsageGraphPath]"
echo "============================================================================="

gnuplot <<-EOFMarker
    set xlabel "Seconds"
    set ylabel "% CPU Usage"
    set title "CPU Usage"
    set term pngcairo size 1680,1050 enhanced font 'Verdana,18'
    set output "${cpuUsageGraphPath}"
    plot for [col=2:4] "${cpuUsagePath}" using 1:col with lines title columnheader
EOFMarker
