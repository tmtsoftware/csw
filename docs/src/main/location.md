# Location service

Location Service handles component (i.e., Applications, Sequencers, Assemblies, HCDs, and Services) registration and discovery in the distributed TMT software system. A component’s location information can be utilized by other component/service to connect or use it. Example of location information is
 
* host address/port pairs
* URL/URIs
* connection protocols

## Artifacts

sbt
:   @@@vars
    ```scala
    libraryDependencies += "org.tmt" %% "csw-location_$scala.binaryVersion$" % "$version$"
    ```
    @@@

Maven
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

Gradle
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
:   @@snip [LocationServiceDemoExample.scala](../../../csw-location/src/test/scala/csw/services/location/scaladsl/demo/LocationServiceDemoExample.scala) { #create-location-service }

Java
:   @@snip [JLocationServiceNonBlockingDemoExample.scala](../../../csw-location/src/test/java/csw/services/location/javadsl/demo/JLocationServiceNonBlockingDemoExample.java) { #create-location-service }


## Shutdown LocationService

This example demonstrates how to shutdown a location service. Shutdown will terminate the ActorSystem and will leave the cluster.  

**Note:** All the services registered via this instance of LocationService will continue to be available for other cluster members. 

Scala
:   @@snip [LocationServiceDemoExample.scala](../../../csw-location/src/test/scala/csw/services/location/scaladsl/demo/LocationServiceDemoExample.scala) { #shutdown }

Java
:   @@snip [JLocationServiceBlockingDemoExample.scala](../../../csw-location/src/test/java/csw/services/location/javadsl/demo/JLocationServiceBlockingDemoExample.java) { #shutdown }

## Creating components, connections and registrations

An Application, Sequencer, Assembly, HCD, or Service component may need to be used by another component as part of normal observatory operations. It must register its location information with Location service so that other components can find it.

**Components** are OMOA entities. They have a name identifier and type such as Container, HCD, Assembly, Service.
   
**Connections** are the means to reach to components and are categorized as Akka, HTTP, Tcp connection.

**Registrations** are service endpoints stored in LocationService.

Scala
:   @@snip [LocationServiceDemoExample.scala](../../../csw-location/src/test/scala/csw/services/location/scaladsl/demo/LocationServiceDemoExample.scala) { #Components-Connections-Registrations }

Java
:   @@snip [JLocationServiceNonBlockingDemoExample.scala](../../../csw-location/src/test/java/csw/services/location/javadsl/demo/JLocationServiceNonBlockingDemoExample.java) { #Components-Connections-Registrations }

## Creating ActorRef for registration

While creating `akkaRegistration` in above example, make sure the ActorSystem used for creating `actorRef`,
 is created using `ActorSystemFactory` as follows :
 

Scala
:  @@snip [LocationServiceDemoExample.scala](../../../csw-location/src/test/scala/csw/services/location/scaladsl/demo/LocationServiceDemoExample.scala) { #create-actor-system }

Java
:  @@snip [JLocationServiceNonBlockingDemoExample.scala](../../../csw-location/src/test/java/csw/services/location/javadsl/demo/JLocationServiceNonBlockingDemoExample.java) { #create-actor-system }

This is required to start a remote ActorSystem on the interface where csw-cluster is running. All the ActorRefs created using this
ActorSystem will now be available for communication from other components that are part of csw-cluster.



## Basic operations

`register` API takes a `Registration` parameter and returns a handle to registration result. The success of `register` API can be validated by checking the `Location` instance pointed by registration result.

The `list` API returns a list of alive connections with LocationService.
  
A connection of interest, can be checked if available using the `resolve` API. If the connection is alive, `resolve` returns the handle to the `Location`.

One of the ways to `unregister` a service is by calling unregister on registration result received from `register` API.

Scala
:   @@snip [LocationServiceDemoExample.scala](../../../csw-location/src/test/scala/csw/services/location/scaladsl/demo/LocationServiceDemoExample.scala) { #register-list-resolve-unregister }

Java
:   @@snip [JLocationServiceNonBlockingDemoExample.scala](../../../csw-location/src/test/java/csw/services/location/javadsl/demo/JLocationServiceNonBlockingDemoExample.java) { #register-list-resolve-unregister }

JavaBlocking
:   @@snip [JLocationServiceBlockingDemoExample.scala](../../../csw-location/src/test/java/csw/services/location/javadsl/demo/JLocationServiceBlockingDemoExample.java) { #register-list-resolve-unregister }

@@@ note { title="async handling in scala and java examples." }

 * **scala:** `async` marks a block of asynchronous code and allows to `await` the computation till the Future is complete.
      For more info, please refer: https://github.com/scala/async
 
 * **java non-blocking example:** The code snippets use `CompletiableFuture` and it's `thenAsync`, `thenApply` methods. This style allows to compose multiple Futures and not block the calling thread till Futures are complete. 

 * **java blocking example:** The code snippets use `CompletableFuture` using `get` blocking call. This style blocks the calling thread till the Future is complete.
    
@@@

## Tracking

The lifecycle of a connection of interest can be followed using `track` API which takes a `Connection` instance as a parameter. **A `Connection` need not already be registered with LocationService.** It's alright to track connections that will be registered in future. 

A `track` API returns two values:     
* A **source** that will emit stream of `TrackingEvents` for the connection.  
* A **Killswitch** to turn off the stream when no longer needed.  

Akka stream API provides many building blocks to process this stream such as Flow and Sink. In example, Sink is used to print each incoming `TrackingEvent`.

Consumer can shut down the stream using Killswitch.

Scala
:   @@snip [LocationServiceDemoExample.scala](../../../csw-location/src/test/scala/csw/services/location/scaladsl/demo/LocationServiceDemoExample.scala) { #tracking }

Java
:   @@snip [JLocationServiceNonBlockingDemoExample.scala](../../../csw-location/src/test/java/csw/services/location/javadsl/demo/JLocationServiceNonBlockingDemoExample.java) { #tracking }

JavaBlocking
:   @@snip [JLocationServiceBlockingDemoExample.scala](../../../csw-location/src/test/java/csw/services/location/javadsl/demo/JLocationServiceBlockingDemoExample.java) { #tracking }

## Subscribing

The `subscribe` API allows the caller to track a connetion and receive the TrackingEvent notifications via a callback. 

The API expects following parameters :    
* An existing connection or a connection to be registered in future.  
* A callback that implements `Consumer`, receives TrackEvent as parameter.  
 
In return it gives a Killswitch that can be used to turn off the event notifications and release the supplied callback, if required.
 
Scala
:   @@snip [LocationServiceDemoExample.scala](../../../csw-location/src/test/scala/csw/services/location/scaladsl/demo/LocationServiceDemoExample.scala) { #subscribing }

Java
:   @@snip [JLocationServiceNonBlockingDemoExample.scala](../../../csw-location/src/test/java/csw/services/location/javadsl/demo/JLocationServiceNonBlockingDemoExample.java) { #subscribing }


## Filtering

The `list` API and it's variants offer means to inquire about available connections with LocationService. The **parameter-less** `list` returns all available connections

Other variants are filters using `ConnectionType`, `ComponentType` and `hostname`.
 
Scala
:   @@snip [LocationServiceDemoExample.scala](../../../csw-location/src/test/scala/csw/services/location/scaladsl/demo/LocationServiceDemoExample.scala) { #filtering }

Java
:   @@snip [JLocationServiceNonBlockingDemoExample.scala](../../../csw-location/src/test/java/csw/services/location/javadsl/demo/JLocationServiceNonBlockingDemoExample.java) { #filtering }

JavaBlocking
:   @@snip [JLocationServiceBlockingDemoExample.scala](../../../csw-location/src/test/java/csw/services/location/javadsl/demo/JLocationServiceBlockingDemoExample.java) { #filtering }

## Source code for examples

* @github[Scala Example](/csw-location/src/test/scala/csw/services/location/scaladsl/demo/LocationServiceDemoExample.scala)
* @github[Java Example](/csw-location/src/test/java/csw/services/location/javadsl/demo/JLocationServiceNonBlockingDemoExample.java)
* @github[JavaBlocking Example](/csw-location/src/test/java/csw/services/location/javadsl/demo/JLocationServiceBlockingDemoExample.java)