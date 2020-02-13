#!/usr/bin/env bash

DEFAULT_CSW_VERSION="master-SNAPSHOT"

# ================================================ #
# Read csw.version property from build.properties file
# Use DEFAULT_CSW_VERSION if build.properties file does not exist or csw.version property does not present
# ================================================ #
BUILD_PROPERTIES="../project/build.properties"
function read_property() {
    if [ -f "$BUILD_PROPERTIES" ]; then
        grep "${1}" $BUILD_PROPERTIES | cut -d'=' -f2
    else
        return 1
    fi
}

MAYBE_CSW_VERSION=$(read_property 'csw.version') || echo $DEFAULT_CSW_VERSION

if [ -z "$MAYBE_CSW_VERSION" ]; then
    CSW_VERSION=$DEFAULT_CSW_VERSION
else
    CSW_VERSION=$MAYBE_CSW_VERSION
fi
# ================================================ #

function run_csw_services() {
    echo "====== CSW Version [$CSW_VERSION] ====="
    coursier launch -r jitpack com.github.tmtsoftware.csw:csw-services_2.13:"$CSW_VERSION" com.typesafe.akka:akka-http-spray-json_2.13:10.1.11 -- "$@"
}

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
