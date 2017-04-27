#!/usr/bin/env bash

docker pull williamyeh/wrk

SCRIPTS_MAPPING="-v `pwd`/scripts:/scripts"

# Get the ip address of local machine and store it in variable: my_ip
my_ip=$(ifconfig | grep "inet " | grep -Fv 127.0.0.1 | awk '{print $2}')

url="http://${my_ip}:5000/"

args="$@"

docker run --rm  $SCRIPTS_MAPPING williamyeh/wrk $args $url