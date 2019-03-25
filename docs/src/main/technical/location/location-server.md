# CSW Location Server

The @github[csw-location-server](/csw-location/csw-location-server) project
contains the main implementation of the Location Service, including the cluster actor and HTTP server.

Normally one instance of the *location server* will run on each host that is running CSW services (although clients can be configured to use a remote host).
The @github[Main](/csw-location/csw-location-server/src/main/scala/csw/location/server/Main.scala) class delegates the job of creating the cluster actor and HTTP server instance to the @github[ServerWiring](/csw-location/csw-location-server/src/main/scala/csw/location/server/internal/ServerWiring.scala) class.

The default tcp ports for the actor and HTTP servers are specified in @github[application.conf](/csw-location/csw-location-server/src/main/resources/application.conf).

@@@ note
Due to the way random port numbers are used for CSW components, firewalls should be disabled for these systems, 
which are assumed to be in an internal network that is protected from outside access. 
@@@

In order to determine the correct IP address to use for the local host, it is necessary to set the *INTERFACE_NAME* environment variable or property to the
name of the network interface to use (There could be multiple NICs).
The @github[ClusterSettings](/csw-location/csw-location-server/src/main/scala/csw/location/server/commons/ClusterSettings.scala) class uses that information,
along with other settings when starting the cluster actor. 
It also needs to know the *cluster seeds*, a comma separated list of *host:port* values for at least one other actor in the cluster.
This information is needed in order to join the location service cluster. 

The Location Service HTTP server is implemented by the @github[LocationRoutes](/csw-location/csw-location-server/src/main/scala/csw/location/server/http/LocationRoutes.scala) class, which defines the HTTP routes and talks to the cluster actor on the client's behalf. 

### Java API

Since the location *server* is only accessed internally, there is no extra Java API for it. 
The location service *client* and *API* code does provide Java APIs (see below).

### Tests

There are numerous tests for the location server, including multi-jvm tests. The tests can be run with:

    sbt csw-location-server/test:test

for the unit tests, and:

    sbt csw-location-server/multi-jvm:test

for the multi-jvm tests.
