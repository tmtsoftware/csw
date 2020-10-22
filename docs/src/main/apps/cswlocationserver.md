# csw-location-server

Note: Normally you will not need to start this application manually. The csw-services.sh script does this for you. 

This application will start a HTTP CSW Location Server on port 7654 which is required for all Location Service consumers who uses HTTP Location client. 
All components (HCD's, Assemblies, Services etc.) use a local HTTP Location client which expects the Location Server running at localhost:7654. 
In a production environment, it is required that all machines running components should have the HTTP Location Server running locally.

## Running latest release of location-server using Coursier

### 1. Add TMT Apps channel to your local Coursier installation using below command

Channel needs to be added to install application using `cs install`

For developer machine setup,

```bash
cs install --add-channel https://raw.githubusercontent.com/tmtsoftware/osw-apps/master/apps.json
```

For production machine setup,

```bash
cs install --add-channel https://raw.githubusercontent.com/tmtsoftware/osw-apps/master/apps.prod.json
```
### 2. Install location-server app

Following command creates an executable file named location-server in the default installation directory.

```bash
cs install location-server:<version | SHA>
```

One can specify installation directory like following:

```bash
cs install \
    --install-dir /tmt/apps \
    location-server:<version | SHA>
```
Note: If you don't provide the version or SHA in above command, `location-server` will be installed with the latest tagged binary of `csw-location-server`
     

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
//cd to installation directory
cd /tmt/apps

// run location server
./location-server --clusterPort=3552
```

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

1.  Run this command on all machines where you want Location Server in `local-only`
mode and AAS `disabled`.
   
    ```bash
    //cd to installation directory
    cd /tmt/apps
    
    // run location server
    ./location-server --clusterPort=3552 
    ```

2.  Run this command on all machines where you want Location Server in `public mode`
and AAS `enabled`.
    ```bash
    //cd to installation directory
        cd /tmt/apps
    
    // run location server
    ./location-server --clusterPort=3552 --publicNetwork
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

```bash
  //cd to installation directory
  cd /tmt/apps
  
  // run location server
  ./location-server --help
```

### Version
Use the following command to get version information for this app.
  
```bash
  //cd to installation directory
  cd /tmt/apps
  
  // run location server
  ./location-server --version
```

## Running latest master of location-server on developer machine

The CSW Location Server application can be installed as binaries or constructed from source. To download the application,
go to the [CSW Release page](https://github.com/tmtsoftware/csw/releases) and follow instructions.

To run the latest master on dev machine  either use the command `sbt run`, or the command `sbt publishLocal` followed by `cs launch location-server:0.1.0-SNAPSHOT`.

Command line parameters can also be passed while launching SNAPSHOT version using coursier.
```bash
  // run location server using coursier
  cs launch location-server:0.1.0-SNAPSHOT -- --clusterPort=3352
```
 
