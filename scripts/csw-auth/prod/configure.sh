#!/usr/bin/env bash

currentDirectory=$( dirname "${BASH_SOURCE[0]}" )
keycloakDir=${currentDirectory}/keycloak
keycloakVersion=4.6.0
keycloakServer=keycloak-${keycloakVersion}.Final
keycloakServerPath=${keycloakDir}/${keycloakServer}.tar.gz

port=8082
host="0.0.0.0"
userName=""
password=""

# Run from the directory containing the script
cd "$( dirname "${BASH_SOURCE[0]}" )"

function unzipTar {
    echo "Unzipping $keycloakServerPath"
    tar -xzf ${keycloakServer}.tar.gz
    echo "Unzipped $keycloakServerPath"
}

function checkIfKeycloakIsInstalled {
    if test -x ${keycloakDir}/${keycloakServer}; then
    echo "$keycloakServer is already installed"
    elif test -e ${keycloakDir}/${keycloakServer}.tar.gz ; then
    echo "$keycloakServerPath is already downloaded."
    cd ${keycloakDir}
    unzipTar
    else
      echo "Installing $keycloakServer"
      test -d ${keycloakDir} || mkdir -p ${keycloakDir}
      curl https://downloads.jboss.org/keycloak/${keycloakVersion}.Final/${keycloakServer}.tar.gz --output ${keycloakServerPath}
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
    cd ${keycloakDir}/${keycloakServer}/bin
    echo "[INFO] starting server at $host:$port"
    sh add-user-keycloak.sh --user ${userName} -p ${password}
    sh standalone.sh -Djboss.bind.address=${host} -Djboss.http.port=${port} -Dkeycloak.migration.action=import -Dkeycloak.migration.provider=singleFile -Dkeycloak.migration.file=/Users/in-poorvag/TMT/csw-prod/scripts/csw-auth/prod/tmt-realm-export.json
}

function start {
#cd to keycloak dir only once
    parse_cmd_args "$@"
    checkIfKeycloakIsInstalled
    addUserAndStartServer
}

start "$@"