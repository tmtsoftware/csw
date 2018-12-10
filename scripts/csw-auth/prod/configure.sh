#!/usr/bin/env bash

currentDir=$(pwd)
keycloakDir=${currentDir}
keycloakVersion=4.6.0
keycloakBinaryUnzipped=keycloak-${keycloakVersion}.Final
keycloakBinaryZipped=${keycloakBinaryUnzipped}.tar.gz
script_name=$0

port=8081
host="0.0.0.0"
userName=""
password=""

importJsonPath="../conf/auth_service/tmt-realm-export.json"

# Run from the directory containing the script
cd "$( dirname "${BASH_SOURCE[0]}" )"

function unzipTar {
    echo "Unzipping  $keycloakDir/$keycloakBinaryZipped"
    tar -xzf ${keycloakBinaryUnzipped}.tar.gz
    echo "Unzipped $keycloakDir/$keycloakBinaryZipped"
}

function checkIfKeycloakIsInstalled {
    if test -x ${keycloakDir}/${keycloakBinaryUnzipped}; then
    echo "$keycloakBinaryUnzipped is already installed"
    elif test -e ${keycloakDir}/${keycloakBinaryUnzipped}.tar.gz ; then
    echo "$keycloakDir/$keycloakBinaryZipped is already downloaded."
    cd ${keycloakDir}
    unzipTar
    else
      echo "Installing $keycloakBinaryUnzipped"
      test -d ${keycloakDir} || mkdir -p ${keycloakDir}
      curl https://downloads.jboss.org/keycloak/${keycloakVersion}.Final/${keycloakBinaryUnzipped}.tar.gz --output ${keycloakDir}/${keycloakBinaryZipped}
      cd ${keycloakDir}
      unzipTar
    fi
}

function parse_cmd_args {
    while [[ $# -gt 0 ]]
        do
            key="$1"

            case ${key} in
                --port | -p)
                   port=$2
                   ;;
                --host | -h)
                    host=$2
                    ;;
                --dir | -d)
                   keycloakDir=$2
                   ;;
                --user | -u)
                    userName=$2
                    ;;
                --password)
                    password=$2
                    ;;
                --help)
                    usage
                    ;;
            esac
        shift
    done

    if [[ ${userName} == "" ]]; then
         echo "[ERROR] Username is missing. Please provide username (--user | -u)"
         exit 1
    fi

    if [[ ${password} == "" ]]; then
         echo "[ERROR] password is missing. Please provide password (--password)"
         exit 1
    fi

}

function usage {
    echo
    echo -e "usage: $script_name [--port | -p <port>] [--host | -h <host>] [--dir | -d <dir>] [--user | -u <user>] [--password  <password>]\n"

    echo "Options:"
    echo "  --port | -p <port>              start AAS on provided port, default: 8081"
    echo "  --host | -h <host>              start AAS on provided ip address, default: starts on ip associated with provided interface and localhost if to be accessed by same machine"
    echo "  --dir | -d <dir>                installs AAS binary on provided directory, default: current working dir"
    echo "  --user | -u <user_name>         add AAS with provided user as admin"
    echo "  --password <password>           add provided password for admin user"
    exit 1
}


function addAdminUser {
    cd ${keycloakDir}/${keycloakBinaryUnzipped}/bin
    echo "[INFO] Adding user"
    sh add-user-keycloak.sh --user ${userName} -p ${password}
}

function startAndRegister {
    cd ${currentDir}
    pwd
    echo "[INFO] starting server at $host:$port"
    ./csw-location-agent --name AAS --http "auth" -c "sh ${keycloakDir}/${keycloakBinaryUnzipped}/bin/standalone.sh -Djboss.bind.address=${host} -Djboss.http.port=${port} -Dkeycloak.migration.action=import -Dkeycloak.migration.provider=singleFile -Dkeycloak.migration.file=${currentDir}/${importJsonPath}" -p "$port"
}

function start {
    parse_cmd_args "$@"
    checkIfKeycloakIsInstalled
    addAdminUser
    startAndRegister
}

start "$@"