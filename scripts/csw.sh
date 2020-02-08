#!/usr/bin/env bash

CSW_VERSION="2.0.0-RC1"
WORK_DIR="$HOME/.csw-services"
BOOTSTRAP_SCRIPT=$WORK_DIR/scripts/csw-bootstrap.sh
CSW_SERVICES_SCRIPT=$WORK_DIR/scripts/csw-services.sh

function checkout_scripts() {
    if hash svn 2>/dev/null; then
        echo "[INFO] Checking out scripts from csw repo here: $WORK_DIR"
        svn checkout https://github.com/tmtsoftware/csw/trunk/scripts
        echo "[INFO] Checked out scripts"
    else
        echo "[ERROR] svn is not installed, follow installation instructions here http://subversion.apache.org/packages.html"
    fi
}

function run_csw_services() {
    if [ -f "$CSW_SERVICES_SCRIPT" ]; then
        $CSW_SERVICES_SCRIPT "$@"
    else
        checkout_scripts
        $CSW_SERVICES_SCRIPT "$@"
    fi
}

function bootstrap_and_execute() {
    CSW_SERVICES=$WORK_DIR/target/coursier/stage/bin/csw-services.sh
    checkout_scripts

    echo "[INFO] Bootstrapping csw services with version [$CSW_VERSION]"
    $BOOTSTRAP_SCRIPT $CSW_VERSION .

    echo "[INFO] Running csw services with version [$CSW_VERSION]"
    $CSW_SERVICES "$@"
}

mkdir -p "$WORK_DIR"

# Change working directory
pushd "$WORK_DIR" || exit

case "$1" in
stop)
    run_csw_services stop
    ;;
start)
    shift
    arr=("start")

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

    bootstrap_and_execute "${arr[@]}"
    ;;
--help | "" | -h | *)
    run_csw_services --help
    ;;
esac

# Go back to original working directory
popd || exit
