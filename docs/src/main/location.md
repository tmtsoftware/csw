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

This example demonstrates:

* `register` a tcp connection with LocationService instance
* then confirms, the returned result contains tcp connection
* then confirms, the `list` API has tcp location
* then uses `resolve` API, to resolve this tcp connection
* prints to tcp uri for demonstration purpose
* then it shows, one of the ways to `unregister` a service by calling unregister on registrationResult
* remaining code ensures, `list` and `resolve` behave as expected after service unregistration

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

This example:

* begins by `track`ing a TcpConnection. For that purpose, LocationService provides a stream of notification events. Each received event is processed by printing on console. We also acquire a  killSwitch to turn the stream off.
* in next two lines, a tcp and a http connection is `register`ed
* after registration is successful, a tcp registration event is printed on the console.
* connections are unregistered
* stream is shutdown using killSwitch

scala
:   @@snip [LocationServiceDemoExample.scala](../../../csw-location/src/test/scala/csw/services/location/scaladsl/demo/LocationServiceDemoExample.scala) { #tracking }

java
:   @@snip [JLocationServiceDemoExample.scala](../../../csw-location/src/test/java/csw/services/location/javadsl/demo/JLocationServiceDemoExample.java) { #tracking }

## Filtering

This example :

* begins by `register`ing three connections - tcp, http, akka
* then it validates that the `list` api returns all three connections
* then it validates that the `list` api with connection type akka returns correct connection
* then it validates that the `list` api with service type returns only tcp and http connections
* then the `list` api with hostname filter, returns tcp and http connections. Note, the akka connection is not listed here because the actorref is created using ActorSystem from a test class which doesn't have a hostname. 

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
