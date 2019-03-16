#!/usr/bin/env bash

currentDir=$(pwd)
keycloakDir=~
keycloakVersion=5.0.0
keycloakBinaryUnzipped=keycloak-${keycloakVersion}
keycloakBinaryZipped=${keycloakBinaryUnzipped}.tar.gz
script_name=$0

location_http_port=7654
port=8081
host="0.0.0.0"
userName=""
password=""
testMode=false
exampleDemo=false

configImportJsonPath="../conf/auth_service/tmt-realm-export.json"
exampleImportJsonPath="../conf/auth_service/example-realm.json"
standaloneXmlPath="../conf/auth_service/standalone.xml"

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
      curl https://downloads.jboss.org/keycloak/${keycloakVersion}/${keycloakBinaryUnzipped}.tar.gz --output ${keycloakDir}/${keycloakBinaryZipped}.incomplete
      if [[ $? != 0 ]]; then
         echo "[ERROR] Failed to download keycloak"
         exit 1
      fi
      mv ${keycloakDir}/${keycloakBinaryZipped}.incomplete ${keycloakDir}/${keycloakBinaryZipped}
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
                --locationHttpPort)
                    location_http_port=$2
                    ;;
                --testMode)
                    testMode=true
                    ;;
                --exampleMode)
                    exampleDemo=true
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
    echo "  --testMode                      Optional parameter which sets up AAS for config server and cli testing"
    echo "  --exampleMode                   Optional parameter which sets up AAS for example server and react app demo"
    exit 1
}

function setJvmOpts {
JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F '.' '{print $1}')
    #checks if java version is higher than 8.
    if [[ ${JAVA_VER} -gt 8 ]]; then export JAVA_OPTS="--add-modules=java.se"; fi
    echo "JAVA_OPTS set as \"${JAVA_OPTS}\" for Java version ${JAVA_VER}"
}

function addAdminUser {
    cd ${keycloakDir}/${keycloakBinaryUnzipped}/bin
    echo "[INFO] Adding user"
    ./add-user-keycloak.sh --user ${userName} -p ${password}
}

function is_AAS_running {
    local http_code=$(curl -s -o /dev/null -w "%{http_code}" http://${host}:${port}/auth/admin/realms)
    if [[ $http_code -eq 401 ]]; then
        return 0
    else
        return 1
    fi
}

function wait_till_AAS_starts {
    until is_AAS_running; do
        echo AAS still not running, waiting 5 seconds
        sleep 5
    done

    echo AAS is running, proceeding with configuration
}


function addTestUsers {
    cd ${keycloakDir}/${keycloakBinaryUnzipped}/bin
    echo "[INFO] Adding test users"
    if ${exampleDemo} ; then
        ./add-user-keycloak.sh -u test-user -p abcd -r example
    else
        ./add-user-keycloak.sh -u kevin -p abcd -r TMT
        ./add-user-keycloak.sh -u frank -p abcd -r TMT
    fi
}

function associateRoleToTestUsers {
    wait_till_AAS_starts
    cd ${keycloakDir}/${keycloakBinaryUnzipped}/bin
        ./kcadm.sh config credentials --server http://${host}:${port}/auth --realm master --user ${userName} --password ${password}
    if ${exampleDemo} ; then
        echo "[INFO] Associate roles to example users"
        ./kcadm.sh add-roles --uusername test-user --rolename person-role --cclientid example-server -r example
        ./kcadm.sh add-roles --uusername test-user --rolename example-admin-role -r example
    else
        echo "[INFO] Associate roles to test users"
        ./kcadm.sh add-roles --uusername kevin --rolename admin --cclientid csw-config-server -r TMT
    fi
}

function startAndRegister {
    cd ${currentDir}
    echo "[INFO] starting server at $host:$port"
    path=${currentDir}/${configImportJsonPath}
    if ${exampleDemo}; then
        path=${currentDir}/${exampleImportJsonPath}
    fi
    cp ${standaloneXmlPath} ${keycloakDir}/${keycloakBinaryUnzipped}/standalone/configuration/standalone.xml
    ./csw-location-agent --name AAS --http "auth" -c "${keycloakDir}/${keycloakBinaryUnzipped}/bin/standalone.sh -Djboss.bind.address=${host} -Djboss.http.port=${port} -Dkeycloak.migration.action=import -Dkeycloak.migration.provider=singleFile -Dkeycloak.migration.file=$path" -p "$port" -J-Dcsw-location-client.server-http-port=${location_http_port}
}

function start {
    parse_cmd_args "$@"
    checkIfKeycloakIsInstalled
    setJvmOpts
    addAdminUser
    if [[ ${testMode} || ${exampleDemo} ]] ; then addTestUsers ; fi
    if [[ ${testMode} || ${exampleDemo} ]] ; then
     associateRoleToTestUsers &
    fi
    startAndRegister
}

start "$@"
