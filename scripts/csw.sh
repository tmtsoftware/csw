#!/usr/bin/env bash

CSW_VERSION="2.0.0-RC1"
WORK_DIR="$HOME/.csw-services"
mkdir -p "$WORK_DIR"

function checkout_and_execute() {
    if hash svn 2>/dev/null; then
        # Change working directory
        pushd "$WORK_DIR" || exit

        echo "[INFO] Checking out scripts from csw repo here: $WORK_DIR"
        svn checkout https://github.com/tmtsoftware/csw/trunk/scripts
        ./scripts/csw-bootstrap.sh $CSW_VERSION .
        ./target/coursier/stage/bin/csw-services.sh "$@"

        # Go back to original working directory
        popd || exit
    else
        echo "[ERROR] svn is not installed, follow installation instructions here http://subversion.apache.org/packages.html"
    fi
}

case "$1" in
stop)
    "$WORK_DIR"/target/coursier/stage/bin/csw-services.sh stop
    shift
    ;;
*)
    checkout_and_execute "$@"
    shift
    ;;
esac
