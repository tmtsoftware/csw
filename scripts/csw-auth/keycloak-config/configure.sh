#!/bin/sh

#login
/opt/jboss/keycloak/bin/kcadm.sh config credentials --server http://keycloak-server:8080/auth --realm master --user admin --password admin

#map roles to uesrs
/opt/jboss/keycloak/bin/kcadm.sh add-roles --uusername kevin --rolename admin --cclientid csw-config-server -r TMT
