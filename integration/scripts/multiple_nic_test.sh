#!/usr/bin/env bash

RED='\033[0;31m'
YELLOW='\033[1;33m'
ORANGE='\033[0;33m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

HOST_DIR_MAPPING=$1
printf "${YELLOW} Executing multiple nic's test... ${NC}\n"
printf "${PURPLE} Creating docker subnet : tmt_net_1 ${NC}\n"
docker network create --subnet=192.168.10.0/24 tmt_net_1

printf "${PURPLE} Creating another docker subnet : tmt_net_2 ${NC}\n"
docker network create --subnet=192.168.20.0/24 tmt_net_2

docker run -d --name=container_1 --net=tmt_net_1 $HOST_DIR_MAPPING tmt/local-csw-centos tail -f /dev/null
docker run -d --name=container_2 --net=tmt_net_2 $HOST_DIR_MAPPING tmt/local-csw-centos tail -f /dev/null

docker network connect bridge container_1
docker network connect bridge container_2

printf "${YELLOW} Starting Assembly in network : tmt_net_1 ${NC}\n"
docker exec -d container_1 bash -c 'cd /source/integration && sbt "run-main csw.services.integtration.apps.AssemblyApp"'

printf "${PURPLE}------ Waiting for 10 seconds to let Assembly gets started ------${NC}\n"
sleep 10

printf "${YELLOW} Executing test in network : tmt_net_2 ${NC}\n"
docker exec -it container_2 bash -c 'cd /source/integration && sbt -DPORT=2550 "test-only csw.services.integration.LocationServiceMultipleNICTest"'
exit_code=$?

printf "${PURPLE} Stopping and removing containers and networks. ${NC}\n"
docker stop container_1 container_2
docker rm container_1 container_2
docker network rm tmt_net_1 tmt_net_2

exit $exit_code