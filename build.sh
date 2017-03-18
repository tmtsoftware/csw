#!/usr/bin/env bash

RED='\033[0;31m'
YELLOW='\033[1;33m'
ORANGE='\033[0;33m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color
RED='\033[0;31m'
NC='\033[0m' # No Color

#Map local project directories and .ivy directory from host to docker container
#e.g.  ./build.sh '-v /home/unmesh/work/csw-prod:/source:z -v /home/unmesh/.ivy2:/root/.ivy2:z'
HOST_DIR_MAPPING=$1
echo "$HOST_DIR_MAPPING"

if [ "$#" -eq  "0" ]
   then
     printf "${RED} Please provide host directory mappings for source root and .ivy. e.g. ${NC} ./build.sh '-v /home/unmesh/work/csw-prod:/source -v /home/unmesh/.ivy2:/root/.ivy2' \n"
     exit 1
fi

docker build -t tmt/local-csw-centos .

printf "${YELLOW}----------- Starting docker container with name : test -----------${NC}\n"
docker run -d -e TRAVIS_JOB_ID=$TRAVIS_JOB_ID --name build-container $HOST_DIR_MAPPING tmt/local-csw-centos tail -f /dev/null
docker exec build-container bash -c 'cd /source && sbt -Dcheck.cycles=true clean scalastyle'
exit_code_cmd_1=$?
docker exec build-container bash -c 'cd /source && sbt csw-location/test'
exit_code_cmd_2=$?
docker exec build-container bash -c 'cd /source && sbt trackLocation/test'
exit_code_cmd_3=$?
docker exec build-container bash -c 'cd /source && sbt coverageReport'
exit_code_cmd_4=$?
docker exec build-container bash -c 'cd /source && sbt coverageAggregate coveralls;'
docker exec build-container bash -c 'cd /source && ./universal_package.sh'

docker stop build-container
docker rm build-container

if [[ $exit_code_cmd_1 -eq "0" && $exit_code_cmd_2 -eq "0" && $exit_code_cmd_3 -eq "0" && $exit_code_cmd_4 -eq "0" ]]
then
    exit 0
else
    exit 1
fi