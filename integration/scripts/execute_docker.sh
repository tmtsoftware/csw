#!/usr/bin/env bash

#Map local project directories and .ivy directory from host to docker container
#e.g.  ./integration/scripts/execute_docker.sh '-v /home/unmesh/work/csw-prod:/source -v /home/unmesh/.ivy2:/root/.ivy2'
HOST_DIR_MAPPING=$1
echo "$HOST_DIR_MAPPING"

echo "Starting HCD docker container"
docker run -d --rm --name node1 $HOST_DIR_MAPPING centos-csw bash -c 'cd /source && sbt integration/run'

echo "Waiting for 10 seconds"
sleep 10

echo "Starting integration test docker container"
docker run -it --rm --name node2 $HOST_DIR_MAPPING centos-csw bash -c 'cd /source && sbt -DPORT=2552 integration/test'

echo "stopping HCD docker container"
docker stop node1