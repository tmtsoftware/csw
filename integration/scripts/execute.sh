#!/usr/bin/env bash
start_hcd_command='sbt integration/run'
$start_hcd_command &
hcd_app_pid=$!
echo "------------ Starting HCD process with id : $hcd_app_pid ------------"

echo "Sleeping for 20 seconds : "
date
sleep 20
echo "Waking up ..."
date

echo "------------ Starting integration test execution -------------"
integration_test_command='sbt -DPORT=2553 integration/test'
$integration_test_command

echo "Printing processes ..............."
ps -ef
awk -version

echo "killing HCD process : $hcd_app_pid"
kill -9 $hcd_app_pid

echo "Searching for process : sbt"

ps -ef | awk '$NF~"sbt" {print $2}'

echo "Searching for process : integration/run"

ps -ef | awk '$NF~"integration/run" {print $2}'

int_tests_pid=`ps -ef | awk '$NF~"integration/run" {print $2}'`

kill -9 $int_tests_pid