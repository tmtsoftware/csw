#!/usr/bin/env bash

RED='\033[0;31m'
YELLOW='\033[1;33m'
ORANGE='\033[0;33m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

docker build -t tmt/local-csw-centos .

printf "${YELLOW}----------- Starting docker container with name : test -----------${NC}\n"
docker run -it --rm -e TRAVIS_JOB_ID=$TRAVIS_JOB_ID --name test-node tmt/local-csw-centos bash -c 'sbt -Dcheck.cycles=true clean scalastyle test;sbt coverageReport;sbt coverageAggregate coveralls'