# csw-cluster-seed


This application will start a CSW cluster seed required for all clustered services like
Location Service. It is required that you start one or more seed applications before consuming Location Service.
Even though a single seed app is good enough to make use of Location Service it is recommended that 
in production you deploy more than one instance on different machines to get fault tolerance in case one of 
the machines hosting the seed crashes.

## Prerequisite

The command `sbt csw-cluster-seed/universal:publishLocal` will publish necessary artifacts to run csw-cluster-seed application. 

The target of above command is a zip file titled "csw-cluster-seed.zip" and its path will be printed on console. Unzip this file and switch current
working directory to the extracted folder. Choose appropriate instructions from below based on requirement ie single seed or multiple seeds.

Note: An alternative method is to run `sbt publishLocal stage`, which installs all the dependencies locally and also installs all the csw applications
 in `target/universal/stage/bin`.
 
# Environment Variables

Note that before running any CSW applications that use the location service  (including HCDs, assemblies and other services), some environment variables or system properties will need to be set to indicate the correct cluster seeds and possibly the network interface to use. More detailed comments on this can be found in the [ClusterSettings](https://tmtsoftware.github.io/csw-prod/api/scala/csw/services/location/commons/ClusterSettings.html) API documentation.

## If Deploying a Single Seed Application
The steps below describe how to run cluster seed on a single machine. This can be a requirement for testing or demo purpose.

**Preparation**:
Find out the IP address and dedicated port for running the seed. Assume that IP is 192.168.1.21 and port is 3552.

**Provisioning**:
Make sure you have the environment variable clusterSeeds set to the IP address and port as in `192.168.1.21:3552`

**Running**: Switch to application directory and run this command - `./bin/csw-cluster-seed --clusterPort=3552`

## If Deploying Two Seed Applications
The steps below describe how to run cluster seed on multiple machines, which is the recommended set-up for production usage.

**Preparation**:
Identify machines which are to run cluster seeds. Let's assume they are two for now, and the IP address for machine1 is 192.168.1.21 and
for machine2 is 192.168.1.22. Also, they will both have dedicated port 3552 to run the seeds. 

**Provisioning**:
Make sure you have the environment variable clusterSeeds set to `192.168.1.21:3552,192.168.1.22:3552` on **machine1 and machine2**.

Switch to application directory and run this command on **machine1 and machine2** - `./bin/csw-cluster-seed --clusterPort=3552`

Note: In some cases it may be necessary to also specify the network interface to use, 
for example, if the host has multiple network interfaces and the correct one is not used automatically.
In this case, you can set the environment variable `interfaceName` to the name of the network interface to use 
or pass the `--interfaceName=...` command line option.
Use `ifconfig -a` to see a list of the available network interfaces.

## Help
Use the following command to get help on the options available with this app.
  
`./bin/csw-cluster-seed --help`

## Version
Use the following command to get version information for this app.
  
`./bin/csw-cluster-seed --version`
