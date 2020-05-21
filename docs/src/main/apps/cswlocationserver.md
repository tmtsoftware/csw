# csw-location-server

Note: Normally you will not need to start this application manually. The csw-services.sh script does this for you. 

This application will start a HTTP CSW Location Server on port 7654 which is required for all Location Service consumers who uses HTTP Location client. 
All components (HCD's, Assemblies, Services etc.) use a local HTTP Location client which expects the Location Server running at localhost:7654. 
In a production environment, it is required that all machines running components should have the HTTP Location Server running locally.

## Prerequisite

The CSW Location Server application can be installed as binaries or constructed from source. To download the application,
go to the [CSW Release page](https://github.com/tmtsoftware/csw/releases) and follow instructions.

To install from source, the command `sbt csw-location-server/universal:publishLocal` will publish necessary artifacts to run the csw-location-server application. 
The target of the above command is a zip file titled "csw-location-server.zip" and its path will be printed on console. 

Note: An alternative method is to run `sbt publishLocal stage`, which installs all the dependencies locally and also installs all the csw applications
 in `target/universal/stage/bin`.

Unzip either the downloaded or constructed zip file and switch current
working directory to the extracted folder. Choose appropriate instructions from below based on requirement (i.e. single machine or multiple machines).
 
### Starting Location Server on a single machine
The steps below describe how to run the Location Server on a single machine. This can be a requirement for testing or demo purposes.

**Preparation**:
Find out the IP address and dedicated port for running the Location Server. Assume that IP is 192.168.1.21 and port is 3552.

**Provisioning**:
Make sure you set all necessary @ref[environment variables](../deployment/env-vars.md). 

**Running**: Switch to the application directory and run this command - `./bin/csw-location-server --clusterPort=3552`

### Starting Location Server on multiple machines
In production environment, you will need a capability to access resources of HTTP interface of Location Server
from @ref:[public network](../deployment/network-topology.md) and provide Authentication and Authorization for some
critical HTTP resource endpoints. E.g. ability to register/unregister components(which has to undergo maintenance), 
from a system operator machine present in @ref:[public network](../deployment/network-topology.md)

**Preparation**:
Identify machines to run Location Server and form Akka cluster and whose IP and port are known. Let's assume they are 3
for now and IP addresses are machine1 192.168.1.21, machine2 192.168.1.22 and machine3 192.168.1.23. Also, they
will have dedicated port 3552 to run the Location Server akka interface. 

**Provisioning**:
Make sure you set all necessary @ref[environment variables](../deployment/env-vars.md).

`AAS` means Authentication and Authorization Service

1.  Switch to application directory and run this command on all machines where you want Location Server in `local-only`
mode and AAS `disabled`.
    ```
    ./bin/csw-location-server --clusterPort=3552 
    ``` 

2.  Switch to application directory and run this command on all machines where you want Location Server in `public mode`
and AAS `enabled`.
    ```
    ./bin/csw-location-server --clusterPort=3552 --publicNetwork
    ``` 

3.  Once the Akka cluster formation is done, start @ref:[AAS](../services/aas.md) 
on one of the node where Location Server is running in `local-only` mode, so that it can register itself to this
Location Server without the need of authentication and authorization. 

4.  Other Location Server instances including public mode instances will get location of AAS automatically using
 location server akka cluster.

5.  Now if application in @ref:[public network](../deployment/network-topology.md) wants to access protected resources
of Location Server, it can connect to any `public mode` Location Server, pass a valid token and access it.

Note : Resolution of AAS is made `lazy` by intention in location server. It will only be resolved `by need` when
first request on protected resource comes to `public mode` location-server. This helps to resolve
cyclic dependency during startup of `public mode` location-server and AAS(keycloak) registration.

### Help
Use the following command to get help on the options available with this app.
  
`./bin/csw-location-server --help`

### Version
Use the following command to get version information for this app.
  
`./bin/csw-location-server --version`