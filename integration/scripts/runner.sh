#!/usr/bin/env bash

RED='\033[0;31m'
YELLOW='\033[1;33m'
ORANGE='\033[0;33m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

#1. Get the current directory (Root dir : csw-prod) which is used to map to docker container
HOST_DIR_MAPPING="-v $(pwd):/source/csw"
echo $HOST_DIR_MAPPING

#2. Pulls docker image from docker hub (This is a custom image which has sbt, java installed)
docker pull twtmt/scala-sbt:8u141_2.12.3_1.0.2

#3. Start first container and run TromboneHcdApp which acts as a seed
# cmd line param : -DclusterPort=3552 => This will start app on port 3552 and create a cluster with a single node
printf "${YELLOW}----------- Starting HCD App -----------${NC}\n"
docker run -d --name=HCD $HOST_DIR_MAPPING twtmt/scala-sbt:8u141_2.12.3_1.0.2 bash -c 'cd /source/csw && ./target/universal/stage/bin/trombone-h-c-d -DclusterPort=3552'

#4. Store the ip address and port of first container (HCD App) into variable clusterSeeds
clusterSeeds="$(docker inspect --format='{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' HCD):3552"
printf "${PURPLE}----------- Akka Seed Node is : ${clusterSeeds}-----------${NC}\n"
sleep 5

#5. Start second container and run TestService app
# cmd line param :-DclusterSeeds=$clusterSeeds (Ip:Port combination of seed - refer #4)
# with clusterSeeds parameters, this container will join the cluster created at step #3
printf "${YELLOW}----------- Starting Reddis App -----------${NC}\n"
docker run -d --name=Reddis --env clusterSeeds=$clusterSeeds $HOST_DIR_MAPPING twtmt/scala-sbt:8u141_2.12.3_1.0.2 bash -c 'cd /source/csw && ./target/universal/stage/bin/test-service -DclusterSeeds=$clusterSeeds'

sleep 5

#6. Start second container and run Test app which executes LocationServiceIntegrationTest
# cmd line param :-DclusterSeeds=$clusterSeeds (Ip:Port combination of seed - refer #4)
# with clusterSeeds parameters, this container will join the cluster created at step #3
printf "${YELLOW}------ Starting Test App ------${NC}\n"
docker run --name=Test-App --env clusterSeeds=$clusterSeeds $HOST_DIR_MAPPING twtmt/scala-sbt:8u141_2.12.3_1.0.2 bash -c 'cd /source/csw && ./target/universal/stage/bin/test-app -DclusterSeeds=$clusterSeeds'
test_exit_code=$?

printf "${PURPLE}---------- Stopping and Removing all docker containers ---------- ${NC}"
docker stop HCD Reddis Test-App
docker rm HCD Reddis Test-App

exit ${test_exit_code}
