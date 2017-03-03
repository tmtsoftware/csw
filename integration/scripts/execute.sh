#!/usr/bin/env bash
start_hcd_command='sbt integration/run'
$start_hcd_command &
hcd_app_pid=$!
echo "------------ Starting HCD process with id : $hcd_app_pid ------------"
sleep 10


echo "------------ Starting integration test execution -------------"
integration_test_command='sbt -DPORT=2553 integration/test'
$integration_test_command

kill -9 $hcd_app_pid

int_tests_pid=`ps -ef | awk '$NF~"integration/run" {print $2}'`

kill -9 $int_tests_pid