#!/usr/bin/env bash

RED='\033[0;31m'
YELLOW='\033[1;33m'
ORANGE='\033[0;33m'
PURPLE='\033[0;35m'
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

#printf "${YELLOW}------------ Building Docker Image : tmt/csw-centos ------------${NC}\n"
#docker build -t tmt/csw-centos .

printf "${YELLOW}----------- Starting HCD Docker container -----------${NC}\n"
docker run -d --name hcd-node $HOST_DIR_MAPPING tmt/local-csw-centos bash -c 'cd /source/integration && sbt run'
printf "${PURPLE}------ Waiting for 10 seconds to let HCD gets started ------${NC}\n"
sleep 10

printf "${ORANGE}------ Starting tcpdump for hcd-node ------${NC}"
sudo timeout 60 tcpdump -i docker0 -n "(igmp or (multicast and port mdns))"

printf "${YELLOW}------ Starting another Docker container to execute tests ------${NC}\n"
docker run -it --rm --name it-node $HOST_DIR_MAPPING tmt/local-csw-centos bash -c 'cd /source/integration && sbt -DPORT=2552 test'

printf "${PURPLE}---------- killing hcd node ---------- {$NC}"
docker rm -f hcd-node