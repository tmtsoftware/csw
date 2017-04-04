#!/usr/bin/env bash

RED='\033[0;31m'
YELLOW='\033[1;33m'
ORANGE='\033[0;33m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

HOST_DIR_MAPPING=$1
printf "${YELLOW} Executing multiple subnet test... ${NC}\n"
printf "${PURPLE} Creating docker subnet : tmt_net_1 ${NC}\n"

docker pull twtmt/centos-tmt

docker network create -d macvlan --subnet=192.168.210.0/24 --subnet=192.168.220.0/24 --gateway=192.168.210.254 --gateway=192.168.220.254 -o parent=ens3 -o macvlan_mode=bridge macvlan-10

docker run --name=lan10.1 --net=macvlan-10 --ip=192.168.210.10 -d $HOST_DIR_MAPPING twtmt/centos-tmt tail -f /dev/null
docker run --name=lan10.2 --net=macvlan-10 --ip=192.168.220.10 -d $HOST_DIR_MAPPING twtmt/centos-tmt tail -f /dev/null
docker run --name=lan10.3 --net=macvlan-10 --ip=192.168.210.5 -d $HOST_DIR_MAPPING twtmt/centos-tmt tail -f /dev/null

akkaSeed="$(docker inspect --format='{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' lan10.1):3552"
printf "${PURPLE}----------- Akka Seed Node is : ${akkaSeed}-----------${NC}\n"


printf "${YELLOW}----------- Starting HCD App -----------${NC}\n"
docker exec -d lan10.1 bash -c 'cd /source/csw && ./integration/target/universal/integration-0.1-SNAPSHOT/bin/trombone-h-c-d'

printf "${PURPLE}------ Waiting for 10 seconds to boot up HCD ------${NC}\n"
sleep 10

printf "${YELLOW}----------- Starting Redis App -----------${NC}\n"
docker exec -d --env akkaSeed=$akkaSeed lan10.2 bash -c 'cd /source/csw && ./integration/target/universal/integration-0.1-SNAPSHOT/bin/test-service -DakkaSeed=$akkaSeed'
printf "${PURPLE}------ Waiting for 10 seconds to boot up Reddis ------${NC}\n"
sleep 10

printf "${YELLOW}------ Starting Test App ------${NC}\n"
docker exec --env akkaSeed=$akkaSeed lan10.3 bash -c 'cd /source/csw && ./integration/target/universal/integration-0.1-SNAPSHOT/bin/test-app -DakkaSeed=$akkaSeed'
test_exit_code=$?

printf "${ORANGE}------ [Debug] Inspecting network information ------${NC}"

docker network inspect macvlan-10

printf "${PURPLE}---------- Stopping and Removing all docker containers ---------- ${NC}"

docker stop lan10.1 lan10.2 lan10.3
docker rm lan10.1 lan10.2 lan10.3

docker network rm macvlan-10

exit ${test_exit_code}