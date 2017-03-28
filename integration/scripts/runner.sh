#!/usr/bin/env bash

RED='\033[0;31m'
YELLOW='\033[1;33m'
ORANGE='\033[0;33m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

#Map local project directories and .ivy directory from host to docker container
#e.g.  ./integration/scripts/execute_docker.sh '-v /home/unmesh/work/csw-prod:/source -v /home/unmesh/.ivy2:/root/.ivy2'
HOST_DIR_MAPPING=$1

#if [ "$#" -eq  "0" ]
#   then
#     printf "${RED} Please provide host directory mappings for source root and .ivy. e.g. ${NC} ./integration/scripts/execute_docker.sh '-v /home/unmesh/work/csw-prod:/source -v /home/unmesh/.ivy2:/root/.ivy2' \n"
#     exit 1
#fi

printf "${YELLOW}----------- Starting HCD App -----------${NC}\n"
docker run -d --name=HCD $HOST_DIR_MAPPING tmt/local-csw-centos bash -c 'cd /source/csw && ./integration/target/universal/integration-0.1-SNAPSHOT/bin/trombone-h-c-d'

akkaSeed=$(docker inspect --format='{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' HCD)
printf "${PURPLE}----------- Akka Seed Node is : ${akkaSeed}-----------${NC}\n"
sleep 5

printf "${YELLOW}----------- Starting Reddis App -----------${NC}\n"
docker run -d --name=Reddis --env akkaSeed=$akkaSeed $HOST_DIR_MAPPING tmt/local-csw-centos bash -c 'cd /source/csw && ./integration/target/universal/integration-0.1-SNAPSHOT/bin/test-service -DakkaSeed=$akkaSeed'

sleep 5

printf "${YELLOW}------ Starting Test App ------${NC}\n"
docker run -it --name=Test-App --env akkaSeed=$akkaSeed $HOST_DIR_MAPPING tmt/local-csw-centos bash -c 'cd /source/csw && ./integration/target/universal/integration-0.1-SNAPSHOT/bin/test-app -DakkaSeed=$akkaSeed'
test_exit_code=$?

printf "${PURPLE}---------- Stopping and Removing all docker containers ---------- ${NC}"
docker stop HCD Reddis Test-App
docker rm HCD Reddis Test-App

exit ${test_exit_code}
