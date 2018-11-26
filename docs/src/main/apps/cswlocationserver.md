# csw-location-server

Note: Normally you will not need to start this application manually. The csw-services.sh script does this for you. 

This application will start a HTTP CSW location server on port 7654 which is required for all Location service consumers who uses HTTP Location client. 
All the components (HCD's, Assemblies, Services etc.) uses local HTTP location client which expects location server running at localhost:7654. 
In a production environment, it is required that all machines running components should have HTTP location server running locally.

## Prerequisite

The CSW location server application can be installed as binaries or constructed from source. To download the application,
go to the [CSW Release page](https://tmtsoftware.github.com/csw/releases/) and follow instructions.

To install from source, the command `sbt csw-location-server/universal:publishLocal` will publish necessary artifacts to run csw-location-server application. 
The target of above command is a zip file titled "csw-location-server.zip" and its path will be printed on console. 

Note: An alternative method is to run `sbt publishLocal stage`, which installs all the dependencies locally and also installs all the csw applications
 in `target/universal/stage/bin`.

Unzip either the downloaded or constructed zip file and switch current
working directory to the extracted folder. Choose appropriate instructions from below based on requirement ie single machine or multiple machines.
 
### Starting Location Server on a single machine
The steps below describe how to run location server on a single machine. This can be a requirement for testing or demo purpose.

**Preparation**:
Find out the IP address and dedicated port for running the location server. Assume that IP is 192.168.1.21 and port is 3552.

**Provisioning**:
Make sure you have the environment variable clusterSeeds set to the IP address and port as in `192.168.1.21:3552`

**Running**: Switch to application directory and run this command - `./bin/csw-location-server --clusterPort=3552`

### Starting Location Server on two machines
The steps below describe how to run location server on multiple machines, which is the recommended set-up for production usage.

**Preparation**:
Identify machines which are running location server and whose IP and port are known. Let's assume they are two for now, and the IP address for machine1 is 192.168.1.21 and
for machine2 is 192.168.1.22. Also, they will both have dedicated port 3552 to run the location server. 

**Provisioning**:
Make sure you have the environment variable clusterSeeds set to `192.168.1.21:3552,192.168.1.22:3552` on **machine1 and machine2**.

Switch to application directory and run this command on **machine1 and machine2** - `./bin/csw-location-server --clusterPort=3552`

@@@ note { title=Note }

In some cases it may be necessary to also specify the network interface to use, 
for example, if the host has multiple network interfaces and the correct one is not used automatically.
In this case, you can set the environment variable `interfaceName` to the name of the network interface to use 
or pass the `--interfaceName=...` command line option.
Use `ifconfig -a` to see a list of the available network interfaces.

@@@

### Help
Use the following command to get help on the options available with this app.
  
`./bin/csw-location-server --help`

### Version
Use the following command to get version information for this app.
  
`./bin/csw-location-server --version`
