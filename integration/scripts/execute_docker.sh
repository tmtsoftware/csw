#!/usr/bin/env bash

RED='\033[0;31m'
YELLOW='\033[1;33m'
ORANGE='\033[0;33m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

#printf "${YELLOW}------------ Building Docker Image : tmt/csw-centos ------------${NC}\n"
#docker build -t tmt/csw-centos .

printf "${YELLOW}----------- Starting HCD Docker container -----------${NC}\n"
docker run -d -e no_proxy="*.local, 169.254/16" --name hcd-node -v ~/.ivy2/:/root/.ivy2/ tmt/local-csw-centos bash -c 'cd integration && sbt run'
docker inspect --format='' hcd-node
printf "${PURPLE}------ Waiting for 10 seconds to let HCD gets started ------${NC}\n"
sleep 60

printf "${YELLOW}------ Starting tcpdump"
sudo timeout 60 tcpdump -i docker0 -n "(igmp or (multicast and port mdns))"

printf "${YELLOW}------ Starting another Docker container to execute tests ------${NC}\n"
docker run -it --rm -e no_proxy="*.local, 169.254/16" --name it-node -v ~/.ivy2/:/root/.ivy2/ tmt/local-csw-centos bash -c 'cd integration && sbt -DPORT=2552 test'
docker inspect --format='' it-node
printf "${YELLOW}------------ force killing hcd node"
docker rm -f hcd-node
