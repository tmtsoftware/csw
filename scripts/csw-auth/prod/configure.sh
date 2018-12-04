#!/usr/bin/env bash

currentDirectory=$( dirname "${BASH_SOURCE[0]}" )
keycloakDir=${currentDirectory}/keycloak
keycloakVersion=4.6.0
keycloakServer=keycloak-${keycloakVersion}.Final
keycloakServerPath=${keycloakDir}/${keycloakServer}.tar.gz

# Run from the directory containing the script
cd "$( dirname "${BASH_SOURCE[0]}" )"

function unzipTar {
    echo "Unzipping $keycloakServerPath"
    cd ${keycloakDir}
    tar -xzf ${keycloakServer}.tar.gz
    echo "Unzipped $keycloakServerPath"
}


function checkIfKeycloakIsInstalled {
    if test -x ${keycloakDir}/${keycloakServer}; then
    echo "$keycloakServer is already installed"
    elif test -e ${keycloakDir}/${keycloakServer}.tar.gz ; then
    echo "$keycloakServerPath is already downloaded."
    unzipTar
    else
      echo "Installing $keycloakServer"
      test -d ${keycloakDir} || mkdir -p ${keycloakDir}
      curl https://downloads.jboss.org/keycloak/${keycloakVersion}.Final/${keycloakServer}.tar.gz --output ${keycloakServerPath}
      unzipTar
    fi
}

checkIfKeycloakIsInstalled