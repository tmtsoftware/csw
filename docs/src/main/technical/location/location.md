# Location Service

## Introduction

The Location Service is used to register and find CSW services in the TMT network. A service can be an Akka actor (including CSW components, such as assemblies and HCDs), an HTTP server or a server running on some TCP host and port.

For information on _using_ the Location Service, see these sections:

- @ref:[Location Service](../../services/location.md)
- @ref:[Location Server](../../apps/cswlocationserver.md)
- @ref:[Location Agent](../../apps/cswlocationagent.md)

## Location Service Implementation

Core implementation of location service uses

- [Akka Cluster](https://doc.akka.io/docs/akka/current/index-cluster.html)
- [Conflict Free Replicated Data Types (CRDTs)](https://doc.akka.io/docs/akka/current/typed/distributed-data.html)
- [Akka Http](https://doc.akka.io/docs/akka-http/current/).

You can find more details on how CSW is using this in @ref:[csw-location-server](./location-server.md).

@@@ note
See [here](https://medium.com/@unmeshvjoshi/service-discovery-with-crdts-fb02bb48cfff) for some background on how the choice was made to use an Akka cluster with CRDTs.
@@@

The implementation of the Location Service is split into following four sub-modules:

- @ref:[csw-location-api](./location-api.md) - common API implemented by the `csw-location-server` and `csw-location-client`
- @ref:[csw-location-server](./location-server.md) - think of it as a agent which runs on every machine and exposes HTTP routes which underneath uses akka cluster and distributed data.
- @ref:[csw-location-client](./location-client.md) - lightweight HTTP client for location service
- @ref:[csw-location-agent](./location-agent.md) - an application used to register non-csw services (Http/Tcp).
