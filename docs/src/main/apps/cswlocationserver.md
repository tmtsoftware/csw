# csw-location-server

Note: Normally you will not need to start this application manually. The `csw-services` application does this for you, refer @ref:[here](cswservices.md). 

This application will start a HTTP CSW Location Service on port 7654 which is required for all Location Service consumers who uses HTTP Location client.
All components (HCD's, Assemblies, Services etc.) use a local HTTP Location client which expects the Location Service running at localhost:7654.
In a production environment, it is required that all machines running components should have the HTTP Location Service running locally.

@@@ note

This page assumes that you have already installed and setup @ref:[coursier](csinstallation.md) { open=new }

@@@

## Install csw-location-server app

Following command creates an executable file named csw-location-server in the default installation directory.

```bash
cs install csw-location-server
```

Note: If you don't provide the version or SHA in above command, `csw-location-server` will be installed with the latest tagged binary of `csw-location-server`

Choose appropriate instructions from below based on requirement (i.e. single machine or multiple machines).
 
### Starting Location Server on a single machine
The steps below describe how to run the Location Server on a single machine. This can be a requirement for testing or demo purposes.

**Preparation**:
Find out the IP address and dedicated port for running the Location Server. Assume that IP is 192.168.1.21 and port is 3552.

**Provisioning**:
Make sure you set all necessary @ref[environment variables](../deployment/env-vars.md). 

**Running**: 

This command starts the location server
```bash
// run location server
csw-location-server --clusterPort=3552
```

### Starting Location Server on multiple machines
In production environment, you will need a capability to access resources of HTTP interface of Location Server
from @ref:[public network](../deployment/network-topology.md) and provide Authentication and Authorization for some
critical HTTP resource endpoints. E.g. ability to register/unregister components(which has to undergo maintenance), 
from a system operator machine present in @ref:[public network](../deployment/network-topology.md)

**Preparation**:
Identify machines to run Location Server and form Pekko cluster and whose IP and port are known. Let's assume they are 3
for now and IP addresses are machine1 192.168.1.21, machine2 192.168.1.22 and machine3 192.168.1.23. Also, they
will have dedicated port 3552 to run the Location Server pekko interface. 

**Provisioning**:
Make sure you set all necessary @ref[environment variables](../deployment/env-vars.md).

`AAS` means Authentication and Authorization Service

1.  Run this command on all machines where you want Location Server in `local-only`
mode and AAS `disabled`.
   
    ```bash
    // run location server
    csw-location-server --clusterPort=3552 
    ```

1.  Run this command on all machines where you want Location Server in `public mode`
and AAS `enabled`.

    ```bash
    // run location server
    csw-location-server --clusterPort=3552 --outsideNetwork
    ``` `

3.  Once the Pekko cluster formation is done, start @ref:[AAS](../services/aas.md) 
on one of the node where Location Server is running in `local-only` mode, so that it can register itself to this
Location Server without the need of authentication and authorization. 

4.  Other Location Server instances including public mode instances will get location of AAS automatically using
 location server pekko cluster.

5.  Now if application in @ref:[public network](../deployment/network-topology.md) wants to access protected resources
of Location Server, it can connect to any `public mode` Location Server, pass a valid token and access it.

Note : Resolution of AAS is made `lazy` by intention in location server. It will only be resolved `by need` when
first request on protected resource comes to `public mode` location-server. This helps to resolve
cyclic dependency during startup of `public mode` location-server and AAS(keycloak) registration.

### Help
Use the following command to get help on the options available with this app.

```bash
   csw-location-server --help
```

### Version
Use the following command to get version information for this app.
  
```bash
   csw-location-server --version
```
