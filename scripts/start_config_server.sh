#!/usr/bin/env bash

stop_app() {
    pid=$(lsof -i:$1 -t);
    (kill -SIGINT $pid) 2> /dev/null
}

start_cluster_seed() {
    sbt csw-cluster-seed/clean
    sbt csw-cluster-seed/universal:packageBin
    echo "All" | unzip csw-cluster-seed/target/universal/csw-cluster-seed-0.1-SNAPSHOT.zip -d csw-cluster-seed/target/universal/

     ./csw-cluster-seed/target/universal/csw-cluster-seed-0.1-SNAPSHOT/bin/csw-cluster-seed --clusterPort $1 --clusterSeeds $2 &
}

start_config_server() {
    sbt csw-config-server/clean
    sbt csw-config-server/universal:packageBin
    echo "All" | unzip csw-config-server/target/universal/csw-config-server-0.1-SNAPSHOT.zip -d csw-config-server/target/universal/

    ./csw-config-server/target/universal/csw-config-server-0.1-SNAPSHOT/bin/csw-config-server --port $1 --clusterSeeds $2 $3 &
}

find_my_ip() {
    my_ip=""
    unamestr=`uname`
    if [[ "$unamestr" == 'Linux' ]]; then
       my_ip=$(hostname -i)
    elif [[ "$unamestr" == 'Darwin' ]]; then
        my_ip=$(ifconfig | grep "inet " | grep -Fv 127.0.0.1 | awk '{print $2}')
    else
        echo "Currently supports 'Linux' & 'Darwin' platform only."
        exit 1
    fi
}

stop_app 5552
stop_app 5000

init=$1

find_my_ip
seeds=$my_ip":5552"

start_cluster_seed 5552 $seeds
start_config_server 5000 $seeds $init