#!/usr/bin/env bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
# DO NOT CHANGE THIS => this string is used in publishing.scala file
DEFAULT_CSW_VERSION="master-SNAPSHOT"
SCRIPT_NAME=$0

# ================================================ #
# Read csw.version property from build.properties file
# Use DEFAULT_CSW_VERSION if build.properties file does not exist or csw.version property does not present
# ================================================ #
BUILD_PROPERTIES="$ROOT_DIR/project/build.properties"
CSW_VERSION=$(grep "csw.version" "$BUILD_PROPERTIES" 2>/dev/null | cut -d'=' -f2)

if [ -z "$CSW_VERSION" ]; then
    CSW_VERSION=$DEFAULT_CSW_VERSION
fi
# ================================================ #

CSW_SERVICES_LIB=com.github.tmtsoftware.csw:csw-services_2.13

function run_csw_services() {
    echo "====== CSW Version [$CSW_VERSION] ====="
    "$SCRIPT_DIR"/coursier launch -r jitpack $CSW_SERVICES_LIB:"$CSW_VERSION" -- "$@"
}

function usage() {
    echo
    echo -e "usage: $SCRIPT_NAME COMMAND [--version | -v <CSW_VERSION>]"

    echo "Options:"
    echo "  --version | -v <CSW_VERSION> Optional CSW version number used to start services"
    echo
    echo "Commands:"
    echo "  start      Starts all csw services if no options provided"
    echo "  stop       Stops all csw services, use this only when script is started in the background"
    echo
    echo "Note: for more information, run $SCRIPT_NAME start --help"

    exit 0
}

if [[ $1 == "stop" ]]; then
    jps -m | grep $CSW_SERVICES_LIB | awk '{print $1}' | xargs kill
    exit 0
elif [[ $1 == "--help" || $1 == "-h" ]]; then
    usage
    exit 0
fi

# capture version number and store rest of the arguments to arr variable which then passed to run csw_services.sh
while (("$#")); do
    case "$1" in
    -v | --version)
        CSW_VERSION="$2"
        shift
        shift
        ;;
    *)
        arr+=("$1")
        shift
        ;;
    esac
done

run_csw_services "${arr[@]}"
