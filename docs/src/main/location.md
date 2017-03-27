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


## Location service creation examples

scala
:   @@snip [LocationServiceFactoryTest.scala](../../../csw-location/src/test/scala/csw/services/location/scaladsl/LocationServiceFactoryTest.scala) { #Location-service-creation-using-actor-runtime }

java
:   @@snip [JLocationServiceFactoryTest.java](../../../csw-location/src/test/java/csw/services/location/javadsl/JLocationServiceFactoryTest.java) {#Location-service-creation-using-actor-runtime}

###  ActorRuntime variations in scala

variation1
:   @@snip [LocationServiceFactoryTest.scala](../../../csw-location/src/test/scala/csw/services/location/scaladsl/ActorRuntimeTest.scala) { #actor-runtime-creation }

variation2
:   @@snip [LocationServiceFactoryTest.scala](../../../csw-location/src/test/scala/csw/services/location/scaladsl/ActorRuntimeTest.scala) { #actor-runtime-creation-with-Settings }

variation3
:   @@snip [LocationServiceFactoryTest.scala](../../../csw-location/src/test/scala/csw/services/location/scaladsl/ActorRuntimeTest.scala) { #actor-runtime-creation-with-system }

###  ActorRuntime variations in java
???


## API Usage Examples

### Register a component

An Application, Sequencer, Assembly, HCD, or Service component may need to be used by another component as part of normal operations. It must register its location information with Location service so that other components can find it.
Note : While creating a component to register, make sure the name of the component does not contain any leading or trailing spaces or "-" in it's name.  

#### Register a component offering http endpoint 

scala
:   @@snip [LocationServiceCompTest.scala](../../../csw-location/src/test/scala/csw/services/location/scaladsl/LocationServiceCompTest.scala) { #register_http_connection }


#### Register a component offering tcp endpoint 

scala
:   @@snip [LocationServiceCompTest.scala](../../../csw-location/src/test/scala/csw/services/location/scaladsl/LocationServiceCompTest.scala) { #register_tcp_connection }

#### Register a component offering akka actor reference 

scala
:   @@snip [LocationServiceCompTest.scala](../../../csw-location/src/test/scala/csw/services/location/scaladsl/LocationServiceCompTest.scala) { #register_akka_connection }

### Unregister a component

#### Using RegistrationResult
scala
:   @@snip [LocationServiceCompTest.scala](../../../csw-location/src/test/scala/csw/services/location/scaladsl/LocationServiceCompTest.scala) { #unregister_using_result }


#### Using a Connection
scala
:   @@snip [LocationServiceCompTest.scala](../../../csw-location/src/test/scala/csw/services/location/scaladsl/LocationServiceCompTest.scala) { #unregister_using_connection }


### Unregister all components/services

scala
:   @@snip [LocationServiceCompTest.scala](../../../csw-location/src/test/scala/csw/services/location/scaladsl/LocationServiceCompTest.scala) { #unregister_all_components }

### Resolve a connection

scala
:   @@snip [LocationServiceCompTest.scala](../../../csw-location/src/test/scala/csw/services/location/scaladsl/LocationServiceCompTest.scala) { #resolve_connection }

### List all components

scala
:   @@snip [LocationServiceCompTest.scala](../../../csw-location/src/test/scala/csw/services/location/scaladsl/LocationServiceCompTest.scala) {#list_all_components}

### List all components based on a particular type e.g HCD, Assembly, Services, etc.

scala
:   @@snip [LocationServiceCompTest.scala](../../../csw-location/src/test/scala/csw/services/location/scaladsl/LocationServiceCompTest.scala) {#list_components_using_type}


### List all components based on connection type e.g Akka connection, Http connection or Tcp connection.

scala
:   @@snip [LocationServiceCompTest.scala](../../../csw-location/src/test/scala/csw/services/location/scaladsl/LocationServiceCompTest.scala) {#list_components_using_connection_type}

### List all components based on a hostname

scala
:   @@snip [LocationServiceCompTest.scala](../../../csw-location/src/test/scala/csw/services/location/scaladsl/LocationServiceCompTest.scala) {#list_components_using_hostname}
