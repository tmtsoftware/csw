# Location Service Technical Description

The Location Service is used to register and find CSW services in the local network. A service can be an Akka actor (including CSW components, such as assemblies and HCDs), an HTTP server or a server running on some TCP host and port.

For information on *using* the Location Service, see these sections:
 
* @ref:[CSW Location Service](../../services/location.md) 
* @ref:[CSW Location Server](../../apps/cswlocationserver.md) 
* @ref:[CSW Location Agent](../../apps/cswlocationagent.md) 

## Location Service Implementation

The core feature of the Location Service is an [Akka](https://akka.io/) cluster that uses [Conflict Free Replicated Datasets (CRDTs)](https://doc.akka.io/docs/akka/current/distributed-data.html) to share location information in the network. 

@@@ note
See [here](https://medium.com/@unmeshvjoshi/service-discovery-with-crdts-fb02bb48cfff) for some background on how the choice was made to use an Akka cluster with CRDTs.
@@@

When a CSW component or service starts up, it registers with the Location Service by posting a message to an HTTP API that talks to one of the actors in the cluster - typically one running on the local host. The location information is shared in the cluster and clients anywhere in the network can use the HTTP API to locate the service.

The implementation of the Location Service is split into four subprojects:

* @ref:[csw-location-server](./location-server.md) - the main implementation, including the cluster actor and HTTP server
* @ref:[csw-location-client](./location-client.md) - a wrapper for the HTTP API, used by clients
* @ref:[csw-location-api](./location-api.md) - common API implemented by the csw-location-server and csw-location-client
* @ref:[csw-location-agent](./location-agent.md) - an application used to register non-csw services

