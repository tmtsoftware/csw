#!/usr/bin/env bash

set -e
csw_root="$( cd "$(dirname "$0")" || exit ; pwd -P )"
acceptance_dir="$csw_root"/scripts/acceptance/

${acceptance_dir}/ammonite ${acceptance_dir}/acceptance-tests.sc "$@"
