function is_keycloak_running {
    local http_code=$(curl -s -o /dev/null -w "%{http_code}" http://keycloak-server:8080/auth/admin/realms)
    if [[ $http_code -eq 401 ]]; then
        return 0
    else
        return 1
    fi
}

function configure_keycloak {
    until is_keycloak_running; do
        echo Keycloak still not running, waiting 5 seconds
        sleep 5
    done

    echo Keycloak is running, proceeding with configuration
}

#wait for keycloak
configure_keycloak

#login
/opt/jboss/keycloak/bin/kcadm.sh config credentials --server http://keycloak-server:8080/auth --realm master --user admin --password admin

#map roles to uesrs
/opt/jboss/keycloak/bin/kcadm.sh add-roles --uusername kevin --rolename admin --cclientid csw-config-server -r TMT
