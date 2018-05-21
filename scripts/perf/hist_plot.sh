#!/usr/bin/env bash

timestamp=`date +%F_%H-%M-%S`

output_path="$HOME/perf/histogram_plot_$timestamp.png"

os=`uname`

echo "============================================================================="
echo "Plotting CPU usage graph using gnuplot at [$output_path]"
echo "============================================================================="

#histogram_files="/Users/pritamkadam/TMT/tmtsoftware/csw-prod/csw-event-impl/target/benchmark-results/Aggregated-*"
histogram_files=$1

gnuplot <<- EOF
    FILES = system("ls -1 ${histogram_files}")
    LABEL = system("ls -1 ${histogram_files} | sed -e 's:.*/::' -e 's/Aggregated-//' -e 's/-results.txt//' ")
    set xlabel "Latency Percentiles"
    set ylabel "Latency in us"
    set title "Latency Plot"
    set style line 1 lc rgb '#8b1a0e' pt 1 ps 1 lt 2 lw 4
    set style line 2 lc rgb '#5e9c36' pt 6 ps 1 lt 2 lw 4
    set style line 11 lc rgb '#808080' lt 1
    set border 3 back ls 11
    set tics nomirror
    set style line 12 lc rgb '#808080' lt 0 lw 1
    set grid back ls 12
    set xtics 0,0.05,1
    set term pngcairo size 1680,1050 enhanced font 'Verdana,18'
    set output "${output_path}"
    plot for [i=1:words(FILES)] word(FILES,i) using 2:1 w lp lw 3 pt 5 ps 1 title word(LABEL,i) noenhanced
EOF
