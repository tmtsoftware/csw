#!/usr/bin/env bash

YELLOW='\033[1;33m'
ORANGE='\033[0;33m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Setting default values for seed port and http port if not provided through command line.
seed_port=5552
http_port=5000
init=""

stop_app() {
    pid=$(lsof -i:$1 -t);
    (kill -SIGTERM $pid || kill -9 $pid) 2> /dev/null
}

start_cluster_seed() {
    sbt csw-cluster-seed/clean
    sbt csw-cluster-seed/universal:stage

    printf "${YELLOW}------ Starting Seed node on port $seed_port ------${NC}\n"
     ./target/universal/stage/bin/csw-cluster-seed --clusterPort $1 -DclusterSeeds=$2 &
}

start_config_server() {
    sbt csw-config-server/clean
    sbt csw-config-server/universal:stage

    printf "${YELLOW}------ Starting config http server on port: $http_port ------${NC}\n"
    ./target/universal/stage/bin/csw-config-server --port $1 -DclusterSeeds=$2 $3
}

usage() {
    echo "usage: $programname [--seedPort port] [--httpPort port] [--init]"
    echo "  --seedPort <number>     optional: start seed on provided port, default: 5552"
    echo "  --httpPort <number>     optional: start http config server on provided port, default: 5000"
    echo "  --init                  optional: create new repo, default: false"
    exit 1
}

parse_cmd_args() {

    while [[ $# -gt 0 ]]
    do
    key="$1"

    case $key in
        --seedPort)
        seed_port="$2"
        shift
        ;;
        --httpPort)
        http_port="$2"
        shift
        ;;
        --initRepo)
        init="--initRepo"
        ;;
         --help)
        usage
        ;;
        *)
        echo "Unknown argument provided. Find usage below :"
        usage
        ;;
    esac
    shift
    done

}

programname=$0
# Parse command line arguments
parse_cmd_args "$@"

printf "${PURPLE}------ Stopping any process started on port: $seed_port & $http_port ------${NC}\n"
stop_app $http_port
stop_app $seed_port

# Get the ip address of local machine and store it in variable: my_ip
my_ip=$(ifconfig | grep "inet " | grep -Fv 127.0.0.1 | awk '{print $2}')

seeds="${my_ip}:${seed_port}"

start_cluster_seed $seed_port $seeds

printf "${YELLOW}------ Cluster seed is : ${seeds} ------${NC}\n"

start_config_server $http_port $seeds $init
