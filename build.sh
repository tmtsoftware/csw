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
docker build -t tmt/local-csw-centos .

printf "${YELLOW}----------- Starting docker container with name : test -----------${NC}\n"
docker run -it --rm --name test-node tmt/local-csw-centos bash -c 'cd /source && sbt -Dcheck.cycles=true clean scalastyle test;sbt coverageReport;'
