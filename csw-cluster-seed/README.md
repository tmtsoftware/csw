CSW Cluster Seed
----------------

This is a mini application which will start a csw cluster seed required for all clustered services like
location-service. It is required that you start one or more seed applications before consuming location-service.
Even though a single seed app is good enough to make use of location-service it is recommended that 
in production you deploy more than one instance on different machines to get the fault tolerance in case one of 
the machines hosting the seed crashes.

**Creating the zip containing binary for the cluster seed app**

You run

sbt csw-cluster-seed/universal:publishLocal

This will create a "csw-cluster-seed.zip" and print the path where it is located. Unzip the file and switch current
working directory to the extracted folder and follow the instructions below.


**Deploying a single seed application**

Assume that 192.168.1.21 is the IP of the machine on which the seed app is being started and you wish to deploy it 
on port 3552.

./bin/csw-cluster-seed -DclusterPort=3552 -DclusterSeeds=192.168.1.21:3552

Once the seed app is running, you can start your application consuming the location service as:

/your-application -DclusterSeeds=192.168.1.21:3552

Instead of providing system properties, it is also possible to set environment variable "clusterSeeds" to the 
seed node address.

**Deploying two seed application**

Assume that 192.168.1.21 is the IP of the machine1 and 192.168.1.22 is the IP of the machine2 on which 
the seed app is being started. Also assume that you want to deploy them on port 3552. You do the following:

On machine1

./bin/csw-cluster-seed -DclusterPort=3552 -DclusterSeeds=192.168.1.21:3552,192.168.1.22:3552

On machine2

./bin/csw-cluster-seed -DclusterPort=3552 -DclusterSeeds=192.168.1.21:3552,192.168.1.22:3552

Once both the seed apps are running, you can start your application consuming the location service as:

/your-application -DclusterSeeds=192.168.1.21:3552,192.168.1.22:3552

Note that you need not provide a clusterPort for your app, as it will pick a random port. 
As earlier, instead of providing system properties, it is also possible to set environment variable "clusterSeeds" to the 
seed node address list.
