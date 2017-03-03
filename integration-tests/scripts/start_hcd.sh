#!/usr/bin/env bash
start_hcd_command='sbt integration-tests/run'
$start_hcd_command &
hcd_app_pid=$!
echo "HCD PROCESS IS $hcd_app_pid"
sleep 10

integration_test_command='sbt integration-tests/test'
$integration_test_command


kill -9 $hcd_app_pid

int_tests_pid=`ps -ef | awk '$NF~"integration-tests/run" {print $2}'`

kill -9 $int_tests_pid