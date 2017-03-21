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

docker run -d --name=Assembly --net=tmt_net_1 $HOST_DIR_MAPPING tmt/local-csw-centos tail -f /dev/null
docker run -d --name=Test-App --net=tmt_net_2 $HOST_DIR_MAPPING tmt/local-csw-centos tail -f /dev/null

docker network connect bridge Assembly
docker network connect bridge Test-App

printf "${YELLOW} Starting Assembly in network : tmt_net_1 ${NC}\n"
docker exec -d Assembly bash -c 'cd source && export PORT=2555;./integration/target/universal/integration-10000/bin/assembly-app'

printf "${PURPLE}------ Waiting for 10 seconds to let Assembly gets started ------${NC}\n"
sleep 10

printf "${YELLOW} Executing test in network : tmt_net_2 ${NC}\n"
docker exec Test-App bash -c 'cd source && export PORT=2557;./integration/target/universal/integration-10000/bin/test-mulitple-nic-app'
exit_code=$?

printf "${ORANGE}------ [Debug] Inspecting docker bridge ------${NC}"
brctl show
docker inspect Assembly
docker inspect Test-App

printf "${PURPLE} Stopping and removing containers and networks. ${NC}\n"

docker stop Assembly Test-App
docker rm Assembly Test-App
docker network rm tmt_net_1 tmt_net_2

exit $exit_code