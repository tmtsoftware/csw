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

scala
:   @@snip [LocationServiceDemoExample.scala](../../../csw-location/src/test/scala/csw/services/location/scaladsl/demo/LocationServiceDemoExample.scala) { #create-location-service }

java
:   @@snip [JLocationServiceDemoExample.scala](../../../csw-location/src/test/java/csw/services/location/javadsl/demo/JLocationServiceDemoExample.java) { #create-location-service }


## Register a component

An Application, Sequencer, Assembly, HCD, or Service component may need to be used by another component as part of normal operations. It must register its location information with Location service so that other components can find it.

#### Components, Connections and Registrations

* Components are OMOA entities. They have a name identifier and type such as HCD, Assembly, Service.   
* Connections are the means to reach to components and are categorized as Akka, HTTP, Tcp connection.
* Registrations are service endpoints stored in LocationService.

scala
:   @@snip [LocationServiceDemoExample.scala](../../../csw-location/src/test/scala/csw/services/location/scaladsl/demo/LocationServiceDemoExample.scala) { #Components-Connections-Registrations }

java
:   @@snip [JLocationServiceDemoExample.scala](../../../csw-location/src/test/java/csw/services/location/javadsl/demo/JLocationServiceDemoExample.java) { #Components-Connections-Registrations }


## Basic operations

* `register` API takes a `Registration` parameter and returns a `Future` of `RegistrationResult`. 
* The success of `register` API can be validated by checking the `Location` instance it contains.
* The `list` API returns a list of alive connections.  
* A connection of interest, can be checked if available using the `resolve` API. If the connection is alive, `resolve` returns Future of `RegistrationResult` containing the `Location`, else failure.
* One of the ways to `unregister` a service is by calling unregister on registrationResult received from `register` API.

scala
:   @@snip [LocationServiceDemoExample.scala](../../../csw-location/src/test/scala/csw/services/location/scaladsl/demo/LocationServiceDemoExample.scala) { #register-list-resolve-unregister }

java
:   @@snip [JLocationServiceDemoExample.scala](../../../csw-location/src/test/java/csw/services/location/javadsl/demo/JLocationServiceDemoExample.java) { #register-list-resolve-unregister }


@@@ note { title="Short note on scala async" }

Below code snippets use scala's async library to demonstrate LocationService API. The use of this library is optional and the API will work just fine with higher order functions provided by scala. 

However, this library makes it easy to work with asynchronous code portions. The `async` marks a block of asynchronous code. It contains one or more await calls, which marks a point at which the computation will be suspended until the awaited Future is complete.

For more info, please refer: https://github.com/scala/async

@@@

## Tracking

* The lifecycle of a connection of interest can be followed using `track` API which takes a `Connection` instance as a parameter. This Connection being tracked need not already be registered with LocationService. It's alright to track connections that will be registered in future. In return, caller gets two values: 
     1. A **source** that will emit stream of `TrackingEvents` for the connection
     2. A **Killswitch** to turn off the stream when no longer needed.
* Akka stream API provides many building blocks to process this stream such as Flow and Sink. In example, Sink is used to print each incoming `TrackingEvent`.
* Consumer can shut down the stream using Killswitch.

scala
:   @@snip [LocationServiceDemoExample.scala](../../../csw-location/src/test/scala/csw/services/location/scaladsl/demo/LocationServiceDemoExample.scala) { #tracking }

java
:   @@snip [JLocationServiceDemoExample.scala](../../../csw-location/src/test/java/csw/services/location/javadsl/demo/JLocationServiceDemoExample.java) { #tracking }

## Filtering

* The `List` API and it's variants offer means to inquire about available connections with LocationService.
* The parameterless list returns all available connections
* The `list` api with connection type returns connections matching the `ConnectionType`. In example it is demonstrated using 'ConnectionType.AkkaType'.
* Similarly, filtering using 'ComponentType.Service' and hostname is also supported by `list` API.
* Note, in the example the akka connection is not listed when filtered using hostname. This is because the actorref is created using local ActorSystem in a test class which doesn't have a hostname. 

scala
:   @@snip [LocationServiceDemoExample.scala](../../../csw-location/src/test/scala/csw/services/location/scaladsl/demo/LocationServiceDemoExample.scala) { #filtering }

java
:   @@snip [JLocationServiceDemoExample.scala](../../../csw-location/src/test/java/csw/services/location/javadsl/demo/JLocationServiceDemoExample.java) { #filtering }


## Shutdown

This example demonstrates how to shutdown a location service. 

scala
:   @@snip [LocationServiceDemoExample.scala](../../../csw-location/src/test/scala/csw/services/location/scaladsl/demo/LocationServiceDemoExample.scala) { #shutdown }

java
:   @@snip [JLocationServiceDemoExample.scala](../../../csw-location/src/test/java/csw/services/location/javadsl/demo/JLocationServiceDemoExample.java) { #shutdown }
