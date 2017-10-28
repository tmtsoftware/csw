#!/usr/bin/env bash

# This script exercises below steps :
#    1.  Create a docker subnet=192.168.10.0/24 with name `tmt_net_1`
#    2.  Create another subnet=192.168.20.0/24 with name tmt_net_2
#    3.  Create a container name `Assembly` and attach it to first network (tmt_net_1)
#    4.  Create a container name `Test-App` and attach it to second network (tmt_net_2)
#    5.  At this stage, both the container will have single NIC allocated  by custom docker network and both residing under two different subnets
#    6.  Now attach both the containers to bridge network (This is a default Docker bridge network)
#    7.  At this stage, both containers will have TWO NIC's (one from custom network and one from docker bridge network)
#    8.  Run AssemblyApp.scala with Interface eth1 (docker bridge Interface) on `Assembly` container with clusterPort=3552 (This will act as a seed to form cluster and register assembly with LocationService)
#    9.  Run TestMultipleNicApp.scala with Interface eth1 (docker bridge Interface) on `Test-App` container. (This will resolve/find a assembly connection which is registers on `Assembly` container)

RED='\033[0;31m'
YELLOW='\033[1;33m'
ORANGE='\033[0;33m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

HOST_DIR_MAPPING="-v $(pwd):/source/csw"
echo $HOST_DIR_MAPPING

docker pull twtmt/scala-sbt:8u141_2.12.3_1.0.2

printf "${YELLOW} Executing multiple nic's test... ${NC}\n"
printf "${PURPLE} Creating docker subnet : tmt_net_1 ${NC}\n"
docker network create --subnet=192.168.10.0/24 tmt_net_1

printf "${PURPLE} Creating another docker subnet : tmt_net_2 ${NC}\n"
docker network create --subnet=192.168.20.0/24 tmt_net_2

docker run -itd --name=Assembly --net=tmt_net_1 $HOST_DIR_MAPPING twtmt/scala-sbt:8u141_2.12.3_1.0.2 bash

docker run -itd --name=Test-App --net=tmt_net_2 $HOST_DIR_MAPPING twtmt/scala-sbt:8u141_2.12.3_1.0.2 bash

docker network connect bridge Assembly
docker network connect bridge Test-App

printf "${YELLOW} Starting Assembly in network : tmt_net_1 ${NC}\n"
docker exec -itd Assembly bash -c 'cd /source/csw && ./target/universal/stage/bin/assembly-app -DclusterPort=3552'

printf "${PURPLE}------ Waiting for 10 seconds to let Assembly gets started ------${NC}\n"
sleep 10
printf "${YELLOW} Executing test in network : tmt_net_2 ${NC}\n"
docker exec Test-App bash -c 'cd /source/csw && ./target/universal/stage/bin/test-multiple-nic-app -DclusterSeeds=172.17.0.2:3552'
exit_code=$?

printf "${ORANGE}------ [Debug] Inspecting docker bridge ------${NC}"
#brctl show
docker inspect Assembly
docker inspect Test-App

printf "${PURPLE} Stopping and removing containers and networks. ${NC}\n"

docker stop Assembly Test-App
docker rm Assembly Test-App
docker network rm tmt_net_1 tmt_net_2

exit $exit_code
