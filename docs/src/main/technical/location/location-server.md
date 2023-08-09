# Location Server

## Introduction

The [csw-location-server]($github.dir.base_url$/csw-location/csw-location-server) project contains the main implementation of the Location Service.
Think of it as a agent which is running on every machine.
Normally one instance of the _Location Server_ will run on each host that is running CSW services (although clients can be configured to use a remote host).

## Design

Main building blocks of location service are captured below, we will go through each one of them in following sections:

- [Pekko Cluster](https://doc.pekko.io/docs/pekko/current/index-cluster.html)
- [Conflict Free Replicated Data Types (CRDTs)](https://doc.pekko.io/docs/pekko/current/typed/distributed-data.html): Shares location information within the network.
- [Pekko HTTP](https://doc.pekko.io/docs/pekko-http/current/)
- DeathWatch Actor

![Location Service](../../images/locationservice/location-service.png)

Above diagram shows different parts of Location Service and how it will look like in TMT environment.
On a single physical machine, we can have multiple JVM's (Java Virtual Machines) running. Roughly these JVM's can be categorized into two types:

- `Container`: It can have single or multiple components (HCD, Assembly etc.) running inside it.
- `Location Service`: Think of it as a agent which is running on all the machines in TMT environment.

@@@ note
Here onwards, we will refer to Location Service as agent or server interchangeably. Do not confuse it with @ref:[csw-location-agent](./location-agent.md).
@@@

Let's discuss different components of `Location Server` in following sections:

### Cluster Member

Location Service JVM (precisely Actor System) takes part in [Pekko Cluster](https://doc.pekko.io/docs/pekko/current/index-cluster.html).
By default, this actor system binds to port `3552`. Initially when there is no member in Pekko cluster, node joins itself.
Such a node is referred as seed node (introducer) and the location of this node needs to be known so that other nodes can join to this known address and form a larger cluster.
After the joining process is complete, seed nodes are not special and they participate in the cluster in exactly the same way as other nodes.

Pekko Cluster provides cluster [membership](https://doc.pekko.io/docs/pekko/current/typed/cluster-membership.html) service using [gossip](https://doc.pekko.io/docs/pekko/current/typed/cluster-concepts.html#gossip)
protocols and an automatic [failure detector](https://doc.pekko.io/docs/pekko/current/typed/cluster-concepts.html#failure-detector).

Death watch uses the cluster failure detector for nodes in the cluster, i.e. it detects network failures and JVM crashes, in addition to graceful termination of watched actor.
Death watch generates the `Terminated` message to the watching actor when the unreachable cluster node has been downed and removed. Hence we have kept `auto-down-unreachable-after = 10s` so that in case of failure, interested parties get the death watch notification for the location in around 10s.

### Distributed Data (Replicator)

We use Pekko Distributed Data to share CSW component locations between nodes in an Pekko Cluster. These locations are accessed with an actor called as `replicator` providing a key-value store like API.
We store the following data in this key-value store (distributed data):

- `AllServices`:
  This uses [LWWMap](https://doc.pekko.io/docs/pekko/current/distributed-data.html?language=scala) CRDT from `Connection` to `Location`. `Connection` and `Location` can be one of `Pekko`, `Tcp` or `HTTP` type.
  At any point in time, the value of this map represents all the locations registered with `Location Service` in a TMT environment.

- `Service`:
  This uses [LWWRegister](https://doc.pekko.io/docs/pekko/current/distributed-data.html?language=scala) which holds location of CSW component against unique connection name.

#### Consistency Guarantees

- `WriteMajority`: All the write API's (register, unregister etc.) updates registry (distributed data) with consistency level of `WriteMajority` which means value will immediately be written to a majority of replicas, i.e. at least N/2 + 1 replicas, where N is the number of nodes in the cluster
- `ReadLocal`: All the get API's (find, resolve, list etc.): retrieves value from registry (distributed data) with consistency level of `ReadLocal` which means value will only be read from the local replica

In TMT environment, we do not want two components to be registered with same connection name.
This is achieved by configuring consistency level of `WriteMajority` for `register` API.
Register API guarantees that a component is registered with Location Service and its entry is replicated to at least N/2 + 1 replicas.

Based on above configuration, it is always guaranteed that only one location of a component will exist at any point in time in registry.
Hence it is safe to read location just from local replica with consistency level of `ReadLocal` with the assumption that eventually location will get replicated on this replica if not present when queried.

### Death Watch Actor

Death watch actor registers interest in change notifications for `AllServices` key. Hence on every addition or removal of location, death watch actor receives `Changed[LWWMap[Connection, Location]]` message from where it gets all the current locations.

Death watch actor then starts watching all the new locations. When it receives `Terminated` signal for any of the watched location precisely for actor ref, then it unregister that particular connection from Location Service.

@@@ note
Death watch actor only supports Pekko locations and filters out `tcp` and `http` locations.
@@@

### HTTP Server

Location Service provides HTTP routes to get, register, unregister and track locations. Only one instance of location server is started on port `7654` on evey machine.
Client from same machine running in different processes can connect to `localhost:7654` to access Location Service. In most of the cases, you will not directly talk to this address. You will always use Location Service client provided by CSW which internally connects to `localhost:7654` to access `Location Service`.

### How location tracking works

Below diagram illustrate `Assembly` tracking `HCD`. Use case shown in diagram is when `Assembly` starts tracking, before `HCD` is registered with Location Service.
It also shows the abrupt shutdown of `HCD` and how `Assembly` gets notification of that.

![Location Track](../../images/locationservice/track.png)

Let us go through each action step by step as shown in diagram:

1. `Assembly` starts tracking `HCD` by sending `HTTP` track request using location client to location server.

    1. On receiving track request, location server internally subscribes to the `replicator` using `Service` key as explained in previous section and generates stream of `TrackingEvent`

    1. Server then maps this stream of `TrackingEvent` to [Websocket]($github.base_url$/csw-location/csw-location-server/src/main/scala/csw/location/server/http/LocationStreamRequestHandler.scala)

    1. Server also keeps sending `ServerSentEvent.heartbeat` every `2 seconds` to keep connection alive

1. `HCD` registers with Location Service by sending `register` request to location server.

    1. On receiving register request, location server internally updates both `Service` and `AllServices` keys by sending `update` message to `replicator`

1. Death watch actor is started with Location Service and it gets notification on every component registration.
   In our flow, death watch actor receives notification of `HCD` getting registered with Location Service from previous step and it immediately starts watching death of `HCD`.

1. One of the tasks of `replicator` is to keep replicating `CRDT's` from one node to other.
   In this case, location of `HCD` gets replicated from `Machine 1` to `Machine 2`

1. As soon as `replicator` from `Machine 2` receives `HCD` location, it notifies all the interested parties.

    1. Remember `Step 1` is interested and receives `Changed(key)` message from `replicator` which gets mapped to `TrackingEvent`

    1. Location server then maps it to `LocationUpdated` event and pushes it to `Assembly` via `SSE`

1. Assume that after some time, `HCD` crashes/terminates/throws exception and shutdowns abruptly.

1. As described in `Step 3`, Death watch actor is watching `HCD`.
   On `HCD's` shutdown, death watch actor `unregisters` `HCD` from Location Service by sending update message by removing `HCD's` entry from `replicator`.

1. Eventually this removal of `HCD` gets replicated to `replicator` running on `Machine 2`.

1. On receiving removal of `HCD` location, same actions gets performed as described in `Step 5`.
   In this case, `LocationRemoved` event gets pushed to `Assembly` via `SSE`

@@@ note
At any point in time, `Assembly` can choose to cancel tracking. On cancellation, this persistent connection will be released.
@@@


### Location Service with Authentication and Authorization

`AAS` means Authentication and Authorization Service

* By default when you start `Location Server`, it will start in `local-only` mode with AAS `Disabled` and bind to `127.0.0.1`.

* Use `--outsideNetwork` command line option when @ref:[starting location server](../../apps/cswlocationserver.md) to
start `Location Server` in `public mode` with AAS `enabled` and bind to @ref:[public IP](../../deployment/network-topology.md)

## Internals

The [Main]($github.base_url$/csw-location/csw-location-server/src/main/scala/csw/location/server/Main.scala) class delegates the job of creating the cluster actor and HTTP server instance to the [ServerWiring]($github.base_url$/csw-location/csw-location-server/src/main/scala/csw/location/server/internal/ServerWiring.scala) class.

The default TCP ports for the actor and HTTP servers are specified in [application.conf]($github.base_url$/csw-location/csw-location-server/src/main/resources/application.conf).

@@@ note
Due to the way random port numbers are used for CSW components, firewalls should be disabled for these systems,
which are assumed to be in an internal network that is protected from outside access.
@@@

In order to determine the correct @ref:[private IP](../../deployment/network-topology.md) address to use for the local
host, it is necessary to set the _INTERFACE_NAME_ environment variable or property to the name of the network
interface to use (There could be multiple NICs).The [ClusterSettings]($github.base_url$/csw-location/csw-location-server/src/main/scala/csw/location/server/commons/ClusterSettings.scala) class uses that information, along with other settings when starting the cluster actor.
It also needs to know the _cluster seeds_, a comma separated list of _host:port_ values for at least one other actor in the cluster.
This information is needed in order to join the Location Service cluster.

In order to determine the correct @ref:[public IP](../../deployment/network-topology.md) address to use for the local
host, it is necessary to set the _AAS_INTERFACE_NAME_ environment variable or property to the name of the network
interface to use (There could be multiple NICs). This variable or property is read by [ClusterSettings]($github.base_url$/csw-location/csw-location-server/src/main/scala/csw/location/server/commons/ClusterSettings.scala) when location service
is started. During registration call, a service can choose which network type it wants to register itself with and
location service will register that service to appropriate network. 

The Location Service HTTP server is implemented by the [LocationRequestHandler]($github.base_url$/csw-location/csw-location-server/src/main/scala/csw/location/server/http/LocationRequestHandler.scala) class, 
[LocationWebsocketHandler]($github.base_url$/csw-location/csw-location-server/src/main/scala/csw/location/server/http/LocationStreamRequestHandler.scala) and talks to the cluster actor on the client's behalf.

## Java API

Since the location _server_ is only accessed internally, there is no extra Java API for it.
The location service _client_ and _API_ code does provide Java APIs (see below).

## Tests

There are numerous tests for the location server, including multi-jvm tests. The tests can be run with:

- Unit/Component Tests: `sbt csw-location-server/test:test`

- Multi-Jvm Tests: `sbt integration/multi-jvm:testOnly csw.location*`
