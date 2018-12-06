#!/usr/bin/env bash

currentDir=$(pwd)
keycloakDir=${currentDir}
keycloakVersion=4.6.0
keycloakBinaryUnzipped=keycloak-${keycloakVersion}.Final
keycloakBinaryZipped=${keycloakBinaryUnzipped}.tar.gz

port=8082
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

# ./configure.sh --port 8080 --addr 121.1.1.1 --dir path

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

function addUserAndStartServer {
    cd ${keycloakDir}/${keycloakBinaryUnzipped}/bin
    echo "[INFO] starting server at $host:$port"
    sh add-user-keycloak.sh --user ${userName} -p ${password}
    sh standalone.sh -Djboss.bind.address=${host} -Djboss.http.port=${port} -Dkeycloak.migration.action=import -Dkeycloak.migration.provider=singleFile -Dkeycloak.migration.file=${keycloakDir}/${importJsonPath}
}

function start {
#cd to keycloak dir only once
    parse_cmd_args "$@"
    checkIfKeycloakIsInstalled
    addUserAndStartServer
}

start "$@"