#!/usr/bin/env bash

# DEOPSCSW-19: CRDT with Multiple NICs
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

YELLOW='\033[1;33m'
ORANGE='\033[0;33m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

HOST_DIR_MAPPING="-v$(pwd):/source/csw"
echo "${HOST_DIR_MAPPING}"

SBT_IMG_TAG="hseeberger/scala-sbt:11.0.6_1.3.8_2.13.1"

# colered print
function printc() {
  declare msg=$1
  declare color=$2
  printf "$color $msg $NC\n"
}

function createDockerNetwork() {
  declare name=$1
  declare subnet=$2
  printc "Creating docker subnet : $name" "$PURPLE"
  docker network create --subnet="$subnet" "$name"
}

printc "Executing multiple NIC's test.." "$YELLOW"
createDockerNetwork tmt_net_1 192.168.10.0/24
createDockerNetwork tmt_net_2 192.168.20.0/24

docker run -itd --name=Assembly --net=tmt_net_1 "${HOST_DIR_MAPPING}" ${SBT_IMG_TAG} bash
docker run -itd --name=Test-App --net=tmt_net_2 "${HOST_DIR_MAPPING}" ${SBT_IMG_TAG} bash

docker network connect bridge Assembly
docker network connect bridge Test-App

printc "Starting Assembly in network : tmt_net_1" "${YELLOW}"
docker exec -itd Assembly bash -c 'cd /source/csw && ./target/universal/stage/bin/assembly-app -DTMT_LOG_HOME=/tmt/logs/csw'

printc "------ Waiting 10 seconds for Assembly to gets started ------" "${PURPLE}"
sleep 10
printc "Executing test in network : tmt_net_2" "${YELLOW}"
docker exec Test-App bash -c 'cd /source/csw && ./target/universal/stage/bin/test-multiple-nic-app -DCLUSTER_SEEDS=172.17.0.2:3553 -DINTERFACE_NAME=eth1'
exit_code=$?
printc "Tests exited with code: $exit_code" "${YELLOW}"

#printc "------ [Debug] Inspecting docker bridge ------" "${ORANGE}"
#brctl show
#docker inspect Assembly
#docker inspect Test-App

printc "Stopping and removing containers and networks." "${PURPLE}"
docker stop Assembly Test-App
docker rm Assembly Test-App
docker network rm tmt_net_1 tmt_net_2

exit "$exit_code"
