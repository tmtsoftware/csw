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
docker run -it --rm --name test-node $HOST_DIR_MAPPING tmt/local-csw-centos bash -c 'cd /source && sbt -Dcheck.cycles=true clean scalastyle test;'
docker run -it --rm --name test-node $HOST_DIR_MAPPING tmt/local-csw-centos bash -c 'cd /source && sbt -DenableCoverage=false clean publishLocal;'
