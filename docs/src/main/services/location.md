
# Location service

Location Service handles component (i.e., Applications, Sequencers, Assemblies, HCDs, and Services) registration and discovery in the distributed TMT software system. A component’s location information can be utilized by other component/service to connect or use it. Example of location information is
 
* host address/port pairs
* URL/URIs
* connection protocols

@@@ note { title="async handling in scala and java examples." }

 * **Scala:** `async` marks a block of asynchronous code and allows to `await` the computation till the Future is complete.
      For more info, please refer: https://github.com/scala/async
 
 * **Java non-blocking example:** The code snippets use `CompletiableFuture` and it's `thenAsync`, `thenApply` methods. This style allows to compose multiple Futures and not block the calling thread till Futures are complete. 

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

LocationServiceFactory exposes a make method to create an instance of LocationService. However, the make call will look for configuration settings managed using ClusterSettings. Verify @scaladoc[ClusterSettings](csw/services/location/commons/ClusterSettings) to ensure that LocationService behavior is as expected.

Scala
:   @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/csw/services/location/LocationServiceExampleClientApp.scala) { #create-location-service }

Java
:   @@snip [JLocationServiceExampleClient.java](../../../../examples/src/main/java/csw/services/location/JLocationServiceExampleClient.java) { #create-location-service }


## Shutdown LocationService

This example demonstrates how to shutdown a location service. Shutdown will terminate the ActorSystem and will leave the cluster.  

**Note:** All the services registered via this instance of LocationService will continue to be available for other cluster members. 

Scala
:   @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/csw/services/location/LocationServiceExampleClientApp.scala) { #shutdown }

Java
:   @@snip [JLocationServiceExampleClient.java](../../../../examples/src/main/java/csw/services/location/JLocationServiceExampleClient.java) { #shutdown }

## Creating components, connections and registrations

An Application, Sequencer, Assembly, HCD, or Service component may need to be used by another component as part of normal observatory operations. It must register its location information with Location service so that other components can find it.

**Components** are OMOA entities. They have a name identifier and type such as Container, HCD, Assembly, Service.
   
**Connections** are the means to reach to components and are categorized as Akka, HTTP, Tcp connection.

**Registrations** are service endpoints stored in LocationService.

`register` API takes a `Registration` parameter and returns a handle to registration result. The success of `register` API can be validated by checking the `Location` instance pointed by registration result.

Scala
:   @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/csw/services/location/LocationServiceExampleClientApp.scala) { #Components-Connections-Registrations }

Java
:   @@snip [JLocationServiceExampleClient.java](../../../../examples/src/main/java/csw/services/location/JLocationServiceExampleClient.java) { #Components-Connections-Registrations }

## Creating ActorRef for registration

While creating `akkaRegistration` in above example, make sure the ActorSystem used for creating `actorRef`,
 is created using `ActorSystemFactory` as follows :
 

Scala
:  @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/csw/services/location/LocationServiceExampleClientApp.scala) { #create-actor-system }

Java
:  @@snip [JLocationServiceExampleClient.java](../../../../examples/src/main/java/csw/services/location/JLocationServiceExampleClient.java) { #create-actor-system }

This is required to start a remote ActorSystem on the interface where csw-cluster is running. All the ActorRefs created using this
ActorSystem will now be available for communication from other components that are part of csw-cluster.



## Resolving Connections

`register` API takes a `Registration` parameter and returns a handle to registration result. The success of `register` API can be validated by checking the `Location` instance pointed by registration result.

The `list` API returns a list of alive connections with LocationService.
  
A connection of interest, can be checked if available using the `resolve` or `find` API.    

`resolve` will find the location for a connection from the local cache, if not found waits for the event to arrive within specified time limit. Returns None if both fail.    

`find` will return the location for a connection from the local cache and if not found then returns None.    

Scala
:   @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/csw/services/location/LocationServiceExampleClientApp.scala) { #find }

Java
:   @@snip [JLocationServiceExampleClient.java](../../../../examples/src/main/java/csw/services/location/JLocationServiceExampleClient.java) { #find }

The output for this find operation when the compoment being search for is not registered should be:

```
Attempting to find connection AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly)) ...
Find result: None
```

An example of the resolve command is shown in the following: 

Scala
:   @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/csw/services/location/LocationServiceExampleClientApp.scala) { #resolve }

Java
:   @@snip [JLocationServiceExampleClient.java](../../../../examples/src/main/java/csw/services/location/JLocationServiceExampleClient.java) { #resolve }

The output should be:

```
Attempting to resolve AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly)) with a wait of 30 seconds ...
```

If you then start the LocationServiceExampleComponentApp, the following line will be outputted:
```
Resolve result: LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType
```

If not, eventually the operation will timeout and the output read:
```
Timeout waiting for location AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly)) to resolve.
```

## Filtering

The `list` API and its variants offer means to inquire about available connections with LocationService. The **parameter-less** `list` returns all available connections

Scala
:   @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/csw/services/location/LocationServiceExampleClientApp.scala) { #list }

Java
:   @@snip [JLocationServiceExampleClient.java](../../../../examples/src/main/java/csw/services/location/JLocationServiceExampleClient.java) { #list }

The output should be:

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

The output should be:

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

The output should be:

```
Registered Akka connections:
--- hcd1-hcd-akka, component type=HCD, connection type=AkkaType
--- assembly1-assembly-akka, component type=Assembly, connection type=AkkaType
--- LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType
```

## Tracking and Subscribing

The lifecycle of a connection of interest can be followed using either the `track` API or the `subscribe` API.  

These methods take a `Connection` instance as a parameter. **A `Connection` need not already be registered with LocationService.** It's alright to track connections that will be registered in future. 

A `track` API returns two values:     
* A **source** that will emit stream of `TrackingEvents` for the connection.  
* A **Killswitch** to turn off the stream when no longer needed.  

Akka stream API provides many building blocks to process this stream such as Flow and Sink. In example, Sink is used to print each incoming `TrackingEvent`.

Consumer can shut down the stream using Killswitch.


The `subscribe` API allows the caller to track a connetion and receive the TrackingEvent notifications via a callback. 

The API expects following parameters :    
* An existing connection or a connection to be registered in future.  
* A callback that implements `Consumer`, receives TrackEvent as parameter.  
 
In return it gives a Killswitch that can be used to turn off the event notifications and release the supplied callback, if required.
 

Scala
:   @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/csw/services/location/LocationServiceExampleClientApp.scala) { #tracking }

Java
:   @@snip [JLocationServiceExampleClient.java](../../../../examples/src/main/java/csw/services/location/JLocationServiceExampleClient.java) { #tracking }

The output should be:
```
Starting to track AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly))
Starting a subscription to AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly))
subscription event
Location updated LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType
Location updated LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType
```

If you now stop the LocationServiceExampleComponentApp, it would print to the screen:
```
subscription event
Location removed AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly))
Location removed AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly))
```

If you start the LocationServiceExampleComponentApp again, the output should be:
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