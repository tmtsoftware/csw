#!/usr/bin/env bash

RED='\033[0;31m'
YELLOW='\033[1;33m'
ORANGE='\033[0;33m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

docker ps -a
docker images

printf "${YELLOW}------------ Killing existing docker containers if exists  ------------${NC}\n"
docker rm -f $(docker ps -a -q)

printf "${YELLOW}------------ Checking if Docker image already exists! ------------${NC}\n"
images=$(docker images)
if [[ ${images} == *"tmt/local-csw-centos"* ]]; then
  printf "${YELLOW} Image exists ${NC}\n"
else
    printf "${YELLOW} Image does not exists ${NC}\n"
    printf "${YELLOW}------------ Building Docker Image : tmt/csw-centos ------------${NC}\n"
    docker build -t tmt/local-csw-centos .
fi

printf "${YELLOW}----------- Starting docker container with name : test -----------${NC}\n"
docker run -it --rm -e no_proxy="*.local, 169.254/16" --name test-node tmt/local-csw-centos bash -c 'sbt -Dcheck.cycles=true clean scalastyle test coverageReport coverageAggregate coveralls'