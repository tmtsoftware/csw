csw-cluster-seed application
------------------------

This application will start a csw cluster seed required for all clustered services like
location-service. It is required that you start one or more seed applications before consuming location-service.
Even though a single seed app is good enough to make use of location-service it is recommended that 
in production you deploy more than one instance on different machines to get the fault tolerance in case one of 
the machines hosting the seed crashes.

**Using SBT to create the zip containing binary for the cluster seed app**

The command `sbt csw-cluster-seed/universal:publishLocal` will publish necessary artifacts to run csw-cluster-seed application. 

The target of above command is a zip file titled "csw-cluster-seed.zip" and it's path will be printed on console. Unzip this file and switch current
working directory to the extracted folder. Choose appropriate instructions from below based on requirement ie single seed or multiple seeds.

**If deploying a single seed application**
Steps below describe set-up to run cluster seed on a single machine. This can be a requirement for testing or demo purpose.

Preparation:
Find out the IP address and dedicated port for running the seed. Assume that IP is 192.168.1.21 and port is 3552.

Provisioning:
Make sure you have environment variable clusterSeeds is set to `192.168.1.21:3552`

Running: Switch to application directory and run this command - `./bin/csw-cluster-seed --clusterPort=3552`

**If deploying two seed application**
Steps below describe set-up to run cluster seed on multiple machines which is recommended set-up for production usage.

Preparation:
Identify machines which are to run cluster seeds. Let's assume they are two for now and the IP address for machine1 is 192.168.1.21 and
for machine2 is 192.168.1.22. Also, they will have dedicated port 3552 to run seeds. 

Provisioning:
Make sure you have environment variable clusterSeeds is set to `192.168.1.21:3552,192.168.1.22:3552` on **machine1 and machine2**.

Switch to application directory and run this command on **machine1 and machine2** - `./bin/csw-cluster-seed --clusterPort=3552`

**Help**
Use the following command to get help on the options available with this app
  
./bin/csw-cluster-seed --help

**Version**
Use the following command to get version information for this app
  
./bin/csw-cluster-seed --version
