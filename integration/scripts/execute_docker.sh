#!/usr/bin/env bash

echo "Starting HCD docker container"
docker run -d --rm --name node1 -v /home/unmesh/work/csw-prod:/source -v /home/unmesh/.ivy2:/root/.ivy2 centos-csw bash -c 'cd /source && sbt integration/run'

echo "Starting integration test docker container"
docker run -it --rm --name node2 -v /home/unmesh/work/csw-prod:/source -v /home/unmesh/.ivy2:/root/.ivy2 centos-csw bash -c 'cd /source && sbt -DPORT=2552 integration/test'

echo "stopping HCD docker container"
docker stop node1