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


## API Usage Examples

### Register a component

An Application, Sequencer, Assembly, HCD, or Service component may need to be used by another component as part of normal operations. It must register its location information with Location service so that other components can find it.

#### Register a component offering http endpoint 

scala
:   @@snip [LocationServiceCompDocExamplesTest.scala](../../../csw-location/src/test/scala/csw/services/location/scaladsl/docexamples/LocationServiceCompDocExamplesTest.scala) { #register_http_connection }

java
:   @@snip [JLocationServiceImplDocExamplesTest.java](../../../csw-location/src/test/java/csw/services/location/javadsl/docexamples/JLocationServiceImplDocExamplesTest.java) {#register_http_connection}

#### Register a component offering tcp endpoint 

scala
:   @@snip [LocationServiceCompDocExamplesTest.scala](../../../csw-location/src/test/scala/csw/services/location/scaladsl/docexamples/LocationServiceCompDocExamplesTest.scala) { #register_tcp_connection }

java
:   @@snip [JLocationServiceImplDocExamplesTest.java](../../../csw-location/src/test/java/csw/services/location/javadsl/docexamples/JLocationServiceImplDocExamplesTest.java) {#register_tcp_connection}

#### Register a component offering akka actor reference 

scala
:   @@snip [LocationServiceCompDocExamplesTest.scala](../../../csw-location/src/test/scala/csw/services/location/scaladsl/docexamples/LocationServiceCompDocExamplesTest.scala) { #register_akka_connection }

java
:   @@snip [JLocationServiceImplDocExamplesTest.java](../../../csw-location/src/test/java/csw/services/location/javadsl/docexamples/JLocationServiceImplDocExamplesTest.java) {#register_akka_connection}

### Unregister a component

scala
:   @@snip [LocationServiceCompDocExamplesTest.scala](../../../csw-location/src/test/scala/csw/services/location/scaladsl/docexamples/LocationServiceCompDocExamplesTest.scala) { #register_http_connection }

java
:   @@snip [JLocationServiceImplDocExamplesTest.java](../../../csw-location/src/test/java/csw/services/location/javadsl/docexamples/JLocationServiceImplDocExamplesTest.java) {#register_http_connection}

