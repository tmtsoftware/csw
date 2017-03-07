#!/usr/bin/env bash

RED='\033[0;31m'
NC='\033[0m' # No Color

#Map local project directories and .ivy directory from host to docker container
#e.g.  ./integration/scripts/execute_docker.sh '-v /home/unmesh/work/csw-prod:/source -v /home/unmesh/.ivy2:/root/.ivy2'
HOST_DIR_MAPPING=$1
echo "$HOST_DIR_MAPPING"

if [ "$#" -eq  "0" ]
   then
     printf "${RED} Please provide host directory mappings for source root and .ivy. e.g. ${NC} ./integration/scripts/execute_docker.sh '-v /home/unmesh/work/csw-prod:/source -v /home/unmesh/.ivy2:/root/.ivy2' \n"
     exit 1
fi

echo "Starting HCD docker container"
docker run -d --rm --name node1 $HOST_DIR_MAPPING centos-csw bash -c 'cd /source/integration && sbt run'

echo "Waiting for 10 seconds"
sleep 10

echo "Starting integration test docker container"
docker run -it --rm --name node2 $HOST_DIR_MAPPING centos-csw bash -c 'cd /source/integration && sbt -DPORT=2552 test'

echo "stopping HCD docker container"
docker stop node1