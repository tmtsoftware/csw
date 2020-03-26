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

### Starting Location Server on two machines
The steps below describe how to run the Location Server on multiple machines, which is the recommended set-up for production usage.

**Preparation**:
Identify machines which are running the Location Server and whose IP and port are known. Let's assume they are two for now, and the IP address for machine1 is 192.168.1.21 and
for machine2 is 192.168.1.22. Also, they will both have dedicated port 3552 to run the Location Server. 

**Provisioning**:
Make sure you set all necessary @ref[environment variables](../deployment/env-vars.md).

Switch to application directory and run this command on **machine1 and machine2** - `./bin/csw-location-server --clusterPort=3552`

Note : `Outside` below means any machine not present in this Akka cluster.

In production environment, you may need a capability to access protected resources of Location Server and provide
 Authentication and Authorization for such resources. E.g. ability to register/unregister components(which has to undergo maintenance) from a system operator machine present
 `Outside`.

1. To enable this, an additional command line argument is available when starting Location Server
    1. Default is local-only mode and such location server will not be accessible from `Outside`
    2. Other option is `--publicNetwork`, it means start location server in public mode with auth enabled
2. Switch to application directory and run this command on all machines where you want Location Server in local-only
 mode (authentication and authorization disabled) - `./bin/csw-location-server --clusterPort=3552` 
3. Switch to application directory and run this command on all machines where you want Location Server in public
 mode (authentication and authorization enabled) - `./bin/csw-location-server --clusterPort=3552 --publicNetwork`  
4. Once Akka cluster is up, start @ref:[Authentication and Authorization Service](../services/aas.md) on one of the
 node where Location Server is running in local-only mode, so that it can register itself to this Location Server
  without the need of authentication and authorization. 
5. Other Location Server instances including public mode instances will get location of Authentication and
 Authorization Service automatically.
6. When any application(E.g. Dashboard application used by Operator to monitor System health) wants to access protected
 resource of Location Server, it can connect to any public mode Location Server, pass a valid token and access it.

### Help
Use the following command to get help on the options available with this app.
  
`./bin/csw-location-server --help`

### Version
Use the following command to get version information for this app.
  
`./bin/csw-location-server --version`