#!/usr/bin/env bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
# DO NOT CHANGE THIS => this string is used in publishing.scala file
DEFAULT_CSW_VERSION="master-SNAPSHOT"

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
    "$SCRIPT_DIR"/coursier launch -r jitpack $CSW_SERVICES_LIB:"$CSW_VERSION" com.typesafe.akka:akka-http-spray-json_2.13:10.1.11 -- "$@"
}

# capture version number and store rest of the arguments to arr variable which then passed to run csw_services.sh
while (("$#")); do
    case "$1" in
    -v | --version)
        CSW_VERSION="$2"
        shift
        shift
        ;;
    stop)
        jps -m | grep $CSW_SERVICES_LIB | awk '{print $1}' | xargs kill
        exit 0
        ;;
    *)
        arr+=("$1")
        shift
        ;;
    esac
done

run_csw_services "${arr[@]}"
