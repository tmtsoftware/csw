# Framework for creating components (HCD, Assembly, Container)

## Artifacts

sbt
:   @@@vars
    ```scala
    libraryDependencies += "org.tmt" %% "csw-framework_$scala.binaryVersion$" % "$version$"
    ```
    @@@

maven
:   @@@vars
    ```xml
    <dependency>
     <groupId>org.tmt</groupId>
     <artifactId>csw-framework_$scala.binaryVersion$</artifactId>
     <version>$version$</version>
     <type>pom</type>
    </dependency>
    ```
    @@@

gradle
:   @@@vars
    ```gradle
    dependencies {
      compile group: "org.tmt", name: "csw-framework_$scala.binaryVersion$", version: "$version$"
    }
    ```
    @@@

## Creating an Assembly or Hcd

A component is implemented by extending the `ComponentHandlers` base class. 

Assembly/Scala
:   @@snip [TromboneAssemblyHandlers.scala](../../../csw-vslice/src/main/scala/csw/trombone/assembly/TromboneAssemblyHandlers.scala) { #component-handler }

Assembly/Java
:   @@snip [JTromboneAssemblyHandlers.java](../../../csw-vslice/src/main/java/csw/trombone/assembly/JTromboneAssemblyHandlers.java) { #jcomponent-handler }

Hcd/Scala
:   @@snip [TromboneHcdHandlers.scala](../../../csw-vslice/src/main/scala/csw/trombone/hcd/TromboneHcdHandlers.scala) { #component-handler }

Hcd/Java
:   @@snip [JTromboneHcdHandlers.java](../../../csw-vslice/src/main/java/csw/trombone/hcd/JTromboneHcdHandlers.java) { #jcomponent-handler }

A component can be created by a factory which extends `ComponentBehaviorFactory` base class and provides a definition of `handlers` method to return the appropriate implementation of `ComponentHandlers`.

Assembly/Scala
:   @@snip [TromboneAssemblyHandlers.scala](../../../csw-vslice/src/main/scala/csw/trombone/assembly/TromboneAssemblyHandlers.scala) { #component-factory }

Assembly/Java
:   @@snip [JTromboneAssemblyBehaviorFactory.java](../../../csw-vslice/src/main/java/csw/trombone/assembly/JTromboneAssemblyBehaviorFactory.java) { #jcomponent-factory }

Hcd/Scala
:   @@snip [TromboneHcdHandlers.scala](../../../csw-vslice/src/main/scala/csw/trombone/hcd/TromboneHcdHandlers.scala) { #component-factory }

Hcd/Java
:   @@snip [JTromboneHcdBehaviorFactory.java](../../../csw-vslice/src/main/java/csw/trombone/hcd/JTromboneHcdBehaviorFactory.java) { #jcomponent-factory }
 

### Lifecycle support

#### initialize

The initialize handler is invoked when the component is created. The component can initialize state such as configuration to be fetched
from configuration service, location of components or services to be fetched from location service etc. The API is future based to favour non-blocking 
asynchronous operations.

Assembly/Scala
:   @@snip [TromboneAssemblyHandlers.scala](../../../csw-vslice/src/main/scala/csw/trombone/assembly/TromboneAssemblyHandlers.scala) { #initialize-handler }

Assembly/Java
:   @@snip [JTromboneAssemblyHandlers.java](../../../csw-vslice/src/main/java/csw/trombone/assembly/JTromboneAssemblyHandlers.java) { #jInitialize-handler }

Hcd/Scala
:   @@snip [TromboneHcdHandlers.scala](../../../csw-vslice/src/main/scala/csw/trombone/hcd/TromboneHcdHandlers.scala) { #initialize-handler }

Hcd/Java
:   @@snip [JTromboneHcdHandlers.java](../../../csw-vslice/src/main/java/csw/trombone/hcd/JTromboneHcdHandlers.java) { #jInitialize-handler }


#### onShutdown

#### onGoOffline

#### onGoOnline

### Handling commands

#### validateCommand

#### onSubmit

#### onOneway

### Tracking Connections

#### onLocationTrackingEvent

### Publishing State

## Container for deployment

## Standalone components


