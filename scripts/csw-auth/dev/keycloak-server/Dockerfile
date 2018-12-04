FROM jboss/keycloak

#import TMT realm
ADD tmt-realm-export.json /tmp/
ENV KEYCLOAK_IMPORT /tmp/tmt-realm-export.json

#add super user
ENV KEYCLOAK_USER admin
ENV KEYCLOAK_PASSWORD admin

#add other users
RUN /opt/jboss/keycloak/bin/add-user-keycloak.sh -u kevin -p abcd -r TMT
RUN /opt/jboss/keycloak/bin/add-user-keycloak.sh -u frank -p abcd -r TMT