#!/usr/bin/env bash

RED='\033[0;31m'
YELLOW='\033[1;33m'
ORANGE='\033[0;33m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

sbt clean compile

start_hcd_command='sbt run'
${start_hcd_command} &
hcd_app_pid=$!

printf "${YELLOW}------------ Starting HCD process with id : $hcd_app_pid ------------${NC}\n"

sleep 10

printf "${YELLOW}------------ Starting Integration Test execution ------------${NC}\n"
integration_test_command='sbt -DPORT=2553 test'
${integration_test_command}

if [ -n "${hcd_app_pid}" ]
then
    printf "${PURPLE}Killing HCD process : $hcd_app_pid ${NC}\n"
    kill -9 ${hcd_app_pid}
fi

int_tests_pid=`ps -ef | awk '$NF~"run" {print $2}'`

if [ -n "${int_tests_pid}" ]
then
    printf "${PURPLE}Killing run process : $int_tests_pid ${NC}\n"
    kill -9 ${int_tests_pid}
fi