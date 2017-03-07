#!/usr/bin/env bash

RED='\033[0;31m'
YELLOW='\033[1;33m'
ORANGE='\033[0;33m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

printf "${YELLOW}------------ Building Docker Image------------${NC}\n"
#docker build -t tmt/csw-centos .

printf "${YELLOW}----------- Starting HCD Docker container -----------${NC}\n"
docker run -d --rm --name node1 -v ~/.ivy2/:/root/.ivy2/ tmt/csw-centos bash -c 'sbt run'
printf "${PURPLE}------ Waiting for 10 seconds to let HCD gets started ------${NC}\n"
sleep 10
printf "${YELLOW}----------- Printing logs from HCD Docker container -----------${NC}\n"
docker logs node1

printf "${YELLOW}------ Starting another Docker container to execute tests ------${NC}\n"
docker run -it --rm --name node2 -v ~/.ivy2/:/root/.ivy2/ tmt/csw-centos bash -c 'sbt -DPORT=2552 test'

printf "${PURPLE}------ Stopping Docker container ------${NC}\n"
docker stop node1