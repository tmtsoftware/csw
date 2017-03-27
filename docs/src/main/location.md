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

