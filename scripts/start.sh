#!/usr/bin/env bash

YELLOW='\033[1;33m'
ORANGE='\033[0;33m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Setting default values for seed port and http port if not provided through command line.
seed_port=5552
config_port=5000

shouldStartSeed=false
shouldStartConfig=false
shouldStartRedis=false

cd ..

stop_app() {
    pid=$(lsof -i:$1 -t);
    (kill -SIGTERM $pid || kill -9 $pid) 2> /dev/null
}

isPortProvided(){
    re='^[0-9]+$'
    if ! [[ $1 =~ $re ]] ; then return 1; else return 0; fi
}

stage_all_projects() {
    printf "${YELLOW} Staging all the projects.${NC}\n"
    sbt universal:stage
}

start_seed() {
    printf "${YELLOW} Starting cluster seed on port $seed_port ${NC}\n"
    ./target/universal/stage/bin/csw-cluster-seed --clusterPort $1 -DclusterSeeds=$2 &
}

start_config() {
    printf "${YELLOW} Starting config service on port $config_port ${NC}\n"
    ./target/universal/stage/bin/csw-config-server --port $1 -DclusterSeeds=$2 $3 &
}

start_redis() {
    printf "${YELLOW} Starting redis with default configuration. ${NC}\n"
}

start_services() {
    if [[ "$shouldStartSeed" = true ]]; then stop_app $seed_port; start_seed $seed_port $seeds; fi
    if [[ "$shouldStartConfig" = true ]]; then stop_app $config_port; start_config $config_port $seeds --initRepo; fi
    if [[ "$shouldStartRedis" = true ]]; then start_redis; fi
}

usage() {
    echo "usage: $program_name [--seed <port>] [--config <port>] [--redis]"
    echo "  --seed <seedPort>       optional: start seed on provided port, default: 5552"
    echo "  --config <configPort>   optional: start http config server on provided port, default: 5000"
    echo "  --redis                 optional: start redis"
    exit 1
}

parse_cmd_args() {

    if [[ $# -gt 0 ]]; then

        while [[ $# -gt 0 ]]
        do
        key="$1"

        case $key in
            --seed)
            shouldStartSeed=true
            if isPortProvided $2; then seed_port="$2"; shift; fi
            ;;
            --config)
            shouldStartConfig=true
            if isPortProvided $2; then config_port="$2"; shift; fi
            ;;
            --redis)
            shouldStartRedis=true
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

    else
        shouldStartSeed=true
        shouldStartConfig=true
        shouldStartRedis=true
    fi

}

program_name=$0
# Parse command line arguments
parse_cmd_args "$@"

# Get the ip address of local machine and store it in variable: my_ip
my_ip=$(ifconfig | grep "inet " | grep -Fv 127.0.0.1 | awk '{print $2}')

seeds="${my_ip}:${seed_port}"

stage_all_projects
start_services

