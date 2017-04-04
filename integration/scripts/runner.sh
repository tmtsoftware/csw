#!/usr/bin/env bash

RED='\033[0;31m'
YELLOW='\033[1;33m'
ORANGE='\033[0;33m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

HOST_DIR_MAPPING="-v $(pwd):/source/csw"
echo $HOST_DIR_MAPPING

docker pull twtmt/centos-tmt

printf "${YELLOW}----------- Starting HCD App -----------${NC}\n"
docker run -d --name=HCD $HOST_DIR_MAPPING twtmt/centos-tmt bash -c 'cd /source/csw && ./integration/target/universal/integration-0.1-SNAPSHOT/bin/trombone-h-c-d -DclusterPort=3552'

clusterSeeds="$(docker inspect --format='{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' HCD):3552"
printf "${PURPLE}----------- Akka Seed Node is : ${clusterSeeds}-----------${NC}\n"
sleep 5

printf "${YELLOW}----------- Starting Reddis App -----------${NC}\n"
docker run -d --name=Reddis --env clusterSeeds=$clusterSeeds $HOST_DIR_MAPPING twtmt/centos-tmt bash -c 'cd /source/csw && ./integration/target/universal/integration-0.1-SNAPSHOT/bin/test-service -DclusterSeeds=$clusterSeeds'

sleep 5

printf "${YELLOW}------ Starting Test App ------${NC}\n"
docker run --name=Test-App --env clusterSeeds=$clusterSeeds $HOST_DIR_MAPPING twtmt/centos-tmt bash -c 'cd /source/csw && ./integration/target/universal/integration-0.1-SNAPSHOT/bin/test-app -DclusterSeeds=$clusterSeeds'
test_exit_code=$?

printf "${PURPLE}---------- Stopping and Removing all docker containers ---------- ${NC}"
docker stop HCD Reddis Test-App
docker rm HCD Reddis Test-App

exit ${test_exit_code}
