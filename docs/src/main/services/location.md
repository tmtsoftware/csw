
# Location service

The Location Service handles component (i.e., Applications, Sequencers, Assemblies, HCDs, and Services) registration 
and discovery in the distributed TMT software system. 
A component’s location information can be used by other components and services to connect to it and use it. 
An example of location information is:
 
* host address/port pairs
* URL/URIs paths
* connection protocols

@@@ note { title="async handling in scala and java examples." }

 * **Scala:** `async` marks a block of asynchronous code and allows to `await` the computation till the Future is complete.
      For more info, please refer: https://github.com/scala/async
 
 * **Java non-blocking example:** The code snippets use `CompletableFuture` and it's `thenAsync`, `thenApply` methods. This style allows to compose multiple Futures and not block the calling thread till Futures are complete. 

 * **Java blocking example:** The code snippets use `CompletableFuture` using `get` blocking call. This style blocks the calling thread till the Future is complete.
    
@@@
## Artifacts

sbt
:   @@@vars
    ```scala
    libraryDependencies += "org.tmt" %% "csw-location_$scala.binaryVersion$" % "$version$"
    ```
    @@@

maven
:   @@@vars
    ```xml
    <dependency>
     <groupId>org.tmt</groupId>
     <artifactId>csw-location_$scala.binaryVersion$</artifactId>
     <version>$version$</version>
     <type>pom</type>
    </dependency>
    ```
    @@@

gradle
:   @@@vars
    ```gradle
    dependencies {
      compile group: "org.tmt", name: "csw-location_$scala.binaryVersion$", version: "$version$"
    }
    ```
    @@@

## Create LocationService

Note that before using this API, the [csw-cluster-seed](../apps/cswclusterseed.html) application 
(the main seed node for the location service cluster) should be running at a known location in the 
network (or at multiple locations) and the necessary configuration, environment variables or system 
properties should be defined to point to the correct host and port number(s).

`LocationServiceFactory` provides a make method to create an instance of the LocationService API. 
This call will look for configuration or environment variable settings as described 
here: @scaladoc[ClusterSettings](csw/services/location/commons/ClusterSettings).

Scala
:   @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/csw/services/location/LocationServiceExampleClientApp.scala) { #create-location-service }

Java
:   @@snip [JLocationServiceExampleClient.java](../../../../examples/src/main/java/csw/services/location/JLocationServiceExampleClient.java) { #create-location-service }


## Shutdown LocationService

This example demonstrates how to disconnect from the location service. 
`Shutdown` will terminate the application's ActorSystem leave the cluster.  

**Note:** All the services registered via this instance of LocationService will continue to be available for other cluster members. 

Scala
:   @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/csw/services/location/LocationServiceExampleClientApp.scala) { #shutdown }

Java
:   @@snip [JLocationServiceExampleClient.java](../../../../examples/src/main/java/csw/services/location/JLocationServiceExampleClient.java) { #shutdown }

## Creating components, connections and registrations

An Application, Sequencer, Assembly, HCD, or Service component may need to be used by another component as part of normal observatory operations. 
It must register its location information with Location service so that other components can find it.

**Components** have a name and a type, such as Container, HCD, Assembly, Service.
   
**Connections** are the means to reach components and are categorized as `Akka`, `HTTP`, or `Tcp` type connections.

**Registrations** are service endpoints stored in LocationService.

The `register` API takes a `Registration` parameter and returns a future registration result. 
If registration fails for some reason, the returned future will fail with an exception. 
(Registration will fail if the `csw-cluster-seed` application is
not running or could not be found or if the given component name was already registered.)

Scala
:   @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/csw/services/location/LocationServiceExampleClientApp.scala) { #Components-Connections-Registrations }

Java
:   @@snip [JLocationServiceExampleClient.java](../../../../examples/src/main/java/csw/services/location/JLocationServiceExampleClient.java) { #Components-Connections-Registrations }

## Creating ActorRef for registration

Make sure the ActorSystem used to start actors using the location service is created using `ActorSystemFactory` as follows:
 

Scala
:  @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/csw/services/location/LocationServiceExampleClientApp.scala) { #create-actor-system }

Java
:  @@snip [JLocationServiceExampleClient.java](../../../../examples/src/main/java/csw/services/location/JLocationServiceExampleClient.java) { #create-actor-system }

This is required to start a remote ActorSystem on the same network interface where the csw-cluster is running. 
All the ActorRefs created using this ActorSystem will be available for communication from other components 
that are part of csw-cluster.


## Resolving Connections

The `list` API returns a list of the currently registered connections from the LocationService.
  
A connection of interest can be looked up using the `resolve` or `find` methods:   

`resolve` gets the location for a connection from the local cache. 
If not found in the cache, it waits for the event to arrive within the specified time limit and returns None on failure.    

`find` returns the location for a connection from the local cache and returns None if not found there.    

Scala
:   @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/csw/services/location/LocationServiceExampleClientApp.scala) { #find }

Java
:   @@snip [JLocationServiceExampleClient.java](../../../../examples/src/main/java/csw/services/location/JLocationServiceExampleClient.java) { #find }

The logging output from the above example when the given component is not registered should include:

```
Attempting to find connection AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly)) ...
Find result: None
```

An example of the resolve command is shown in the following: 

Scala
:   @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/csw/services/location/LocationServiceExampleClientApp.scala) { #resolve }

Java
:   @@snip [JLocationServiceExampleClient.java](../../../../examples/src/main/java/csw/services/location/JLocationServiceExampleClient.java) { #resolve }

The logging output from the above example should include:

```
Attempting to resolve AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly)) with a wait of 30 seconds ...
```

If you then start the LocationServiceExampleComponentApp, the following message will be logged:
```
Resolve result: LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType
```

If not, eventually the operation will timeout and the output should read:
```
Timeout waiting for location AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly)) to resolve.
```

## Filtering

The `list` API and its variants offer means to inquire about available connections with the LocationService. 
The **parameter-less** `list` returns all available connections

Scala
:   @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/csw/services/location/LocationServiceExampleClientApp.scala) { #list }

Java
:   @@snip [JLocationServiceExampleClient.java](../../../../examples/src/main/java/csw/services/location/JLocationServiceExampleClient.java) { #list }

The log output from the above should contain:

```
All Registered Connections:
--- hcd1-hcd-akka, component type=HCD, connection type=AkkaType
--- assembly1-assembly-akka, component type=Assembly, connection type=AkkaType
--- redis-service-tcp, component type=Service, connection type=TcpType
--- configuration-service-http, component type=Service, connection type=HttpType
--- LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType
```

Other variants are filters using `ConnectionType`, `ComponentType`, and `hostname`.

Filtering by component type is shown below:

Scala
:   @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/csw/services/location/LocationServiceExampleClientApp.scala) { #filtering-component }

Java
:   @@snip [JLocationServiceExampleClient.java](../../../../examples/src/main/java/csw/services/location/JLocationServiceExampleClient.java) { #filtering-component }

The log output from the above code should contain:

```
Registered Assemblies:
--- assembly1-assembly-akka, component type=Assembly, connection type=AkkaType
--- LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType
```

Filtering by connection type is shown below:

Scala
:   @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/csw/services/location/LocationServiceExampleClientApp.scala) { #filtering-connection }

Java
:   @@snip [JLocationServiceExampleClient.java](../../../../examples/src/main/java/csw/services/location/JLocationServiceExampleClient.java) { #filtering-connection }

The log output should contain:

```
Registered Akka connections:
--- hcd1-hcd-akka, component type=HCD, connection type=AkkaType
--- assembly1-assembly-akka, component type=Assembly, connection type=AkkaType
--- LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType
```

## Tracking and Subscribing

The lifecycle of a connection of interest can be followed using either the `track` API or the `subscribe` API.  

These methods take a `Connection` instance as a parameter. **A `Connection` need not already be registered with LocationService.** 
It's alright to track connections that will be registered in future. 

A `track` API returns two values:     
* A **source** that will emit a stream of `TrackingEvents` for the connection.  
* A **Killswitch** to turn off the stream when no longer needed.  

The Akka stream API provides many building blocks to process this stream, such as Flow and Sink. 
In the example below, `Sink.actorRef` is used to forward any location messages received to the current actor (self).

A consumer can shut down the stream using the Killswitch.


The `subscribe` API allows the caller to track a connection and receive the TrackingEvent notifications via a callback. 

The API expects following parameters :    
* An existing connection or a connection to be registered in the future.  
* A callback that implements `Consumer`, receives the TrackEvent as a parameter.  
 
In return it gives a Killswitch that can be used to turn off the event notifications and release the supplied callback, if required.
 

Scala
:   @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/csw/services/location/LocationServiceExampleClientApp.scala) { #tracking }

Java
:   @@snip [JLocationServiceExampleClient.java](../../../../examples/src/main/java/csw/services/location/JLocationServiceExampleClient.java) { #tracking }

The log output should contain the following:
```
Starting to track AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly))
Starting a subscription to AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly))
subscription event
Location updated LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType
Location updated LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType
```

If you now stop the LocationServiceExampleComponentApp, it would log:
```
subscription event
Location removed AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly))
Location removed AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly))
```

If you start the LocationServiceExampleComponentApp again, the log output should be:
```
Location updated LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType
subscription event
Location updated LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType
```

Note: the line after the words "subscription event" in our example is generated by the subscription, and the other line is from
tracking.  These two events could come in any order.


## Unregistering

One of the ways to `unregister` a service is by calling unregister on registration result received from `register` API.

Scala
:   @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/csw/services/location/LocationServiceExampleClientApp.scala) { #unregister }

Java
:   @@snip [JLocationServiceExampleClient.java](../../../../examples/src/main/java/csw/services/location/JLocationServiceExampleClient.java) { #unregister }


## Source code for examples

* @github[Scala Example](/examples/src/main/scala/csw/services/location/LocationServiceExampleClientApp.scala)
* @github[JavaBlocking Example](/examples/src/main/java/csw/services/location/JLocationServiceExampleClient.java)
