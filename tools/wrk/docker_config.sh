#!/usr/bin/env bash

docker pull twtmt/centos-tmt

docker rm server

ROOT_DIRECTORY="$(dirname "$(dirname $(pwd))")"
HOST_DIR_MAPPING="-v $ROOT_DIRECTORY:/source/csw"
IVY_MAPPING="-v $HOME/.ivy2/:/root/.ivy2"

docker run -it --name server -p 5000:5000 $HOST_DIR_MAPPING $IVY_MAPPING twtmt/centos-tmt bash -c 'cd /source/csw && ./integration/scripts/start_config_server.sh --initRepo'