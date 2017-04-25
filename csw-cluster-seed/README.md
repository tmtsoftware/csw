csw-cluster-seed application
------------------------

This application will start a csw cluster seed required for all clustered services like
location-service. It is required that you start one or more seed applications before consuming location-service.
Even though a single seed app is good enough to make use of location-service it is recommended that 
in production you deploy more than one instance on different machines to get the fault tolerance in case one of 
the machines hosting the seed crashes.

If you want to get started with csw-cluster-seed, refer the [document](https://tmtsoftware.github.io/csw-prod/cswclusterseed.html).
