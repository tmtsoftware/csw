#!/usr/bin/env bash

RED='\033[0;31m'
YELLOW='\033[1;33m'
ORANGE='\033[0;33m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

printf "${YELLOW}------------ Building Docker Image : tmt/csw-centos ------------${NC}\n"
docker build -t tmt/csw-centos .

printf "${YELLOW}----------- Starting docker container with name : test -----------${NC}\n"
docker run -it --rm --name test-node tmt/csw-centos-test bash -c 'sbt -Dcheck.cycles=true clean scalastyle test coverageReport coverageAggregate coveralls'