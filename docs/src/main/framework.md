# Framework for creating components (HCD, Assembly, Container)

## Artifacts

sbt
:   @@@vars
    ```scala
    libraryDependencies += "org.tmt" %% "csw-framework" % "$version$"
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
    
## ComponentInfo

`ComponentInfo` model describes a component by specifying following details
It is usually described as a configuration file but can also be created programmatically.

AssemblyInfo
:   @@@vars
    ```
    name = "Sample_Assembly"
    componentType = assembly
    behaviorFactoryClassName = package.component.SampleAssembly
    prefix = abc.sample.prefix
    locationServiceUsage = RegisterAndTrackServices
    connections = [
        {
          name: "Sample_Assembly"
          componentType: assembly
          connectionType: akka
        }
      ]
    ```
    @@@
    
HcdInfo
:   @@@vars
    ```
    name = "Sample_Hcd"
    componentType = hcd
    behaviorFactoryClassName = package.component.SampleHcd
    prefix = abc.sample.prefix
    locationServiceUsage = RegisterOnly
    ```
    
Following summaries the properties to be defined in the ComponentInfo model:

* **name** : The name of the component
* **componentType** : The type of the component which could be `Container`, `Assembly`, `Hcd` or `Service`
* **behaviorFactoryClassName** : The fully qualified name of the class which extends the factory class `ComponentBehaviorFactory`
* **prefix** : A valid subsystem to which this component belongs.
* **locationServiceUsage** : Indicates how the location service should be leveraged for this component by the framework. Following values are supported:
    * DoNotRegister : Do not register this component with location service
    * RegisterOnly : Register this component with location service
    * RegisterAndTrackServices : Register this component with location service as well as track the components/services mentioned against `connections` property

## Creating an Assembly or Hcd

A component is implemented by extending the `ComponentHandlers` base class. 

Assembly/Scala
:   @@snip [TromboneAssemblyHandlers.scala](../../../csw-vslice/src/main/scala/csw/trombone/assembly/TromboneAssemblyHandlers.scala) { #component-handlers-class }

Assembly/Java
:   @@snip [JTromboneAssemblyHandlers.java](../../../csw-vslice/src/main/java/csw/trombone/assembly/JTromboneAssemblyHandlers.java) { #jcomponent-handlers-class }

Hcd/Scala
:   @@snip [TromboneHcdHandlers.scala](../../../csw-vslice/src/main/scala/csw/trombone/hcd/TromboneHcdHandlers.scala) { #component-handlers-class }

Hcd/Java
:   @@snip [JTromboneHcdHandlers.java](../../../csw-vslice/src/main/java/csw/trombone/hcd/JTromboneHcdHandlers.java) { #jcomponent-handlers-class }

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

The `initialize` handler is invoked when the component is created. The component can initialize state such as configuration to be fetched
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

#### isOnline
A component has access to `isOnline` boolean flag which can be used to determine if the component is online or offline state.

#### onGoOffline
A component can be notified to run in offline mode in case it is not in use. The component can change its behavior if needed as a part of this handler.

Assembly/Scala
:   @@snip [TromboneAssemblyHandlers.scala](../../../csw-vslice/src/main/scala/csw/trombone/assembly/TromboneAssemblyHandlers.scala) { #onGoOffline-handler }

Assembly/Java
:   @@snip [JTromboneAssemblyHandlers.java](../../../csw-vslice/src/main/java/csw/trombone/assembly/JTromboneAssemblyHandlers.java) { #onGoOffline-handler }

Hcd/Scala
:   @@snip [TromboneHcdHandlers.scala](../../../csw-vslice/src/main/scala/csw/trombone/hcd/TromboneHcdHandlers.scala) { #onGoOffline-handler }

Hcd/Java
:   @@snip [JTromboneHcdHandlers.java](../../../csw-vslice/src/main/java/csw/trombone/hcd/JTromboneHcdHandlers.java) { #onGoOffline-handler }

#### onGoOnline
A component can be notified to run in online mode again in case it was put to run in offline mode. The component can change its behavior if needed as a part of this handler.

Assembly/Scala
:   @@snip [TromboneAssemblyHandlers.scala](../../../csw-vslice/src/main/scala/csw/trombone/assembly/TromboneAssemblyHandlers.scala) { #onGoOnline-handler }

Assembly/Java
:   @@snip [JTromboneAssemblyHandlers.java](../../../csw-vslice/src/main/java/csw/trombone/assembly/JTromboneAssemblyHandlers.java) { #onGoOnline-handler }

Hcd/Scala
:   @@snip [TromboneHcdHandlers.scala](../../../csw-vslice/src/main/scala/csw/trombone/hcd/TromboneHcdHandlers.scala) { #onGoOnline-handler }

Hcd/Java
:   @@snip [JTromboneHcdHandlers.java](../../../csw-vslice/src/main/java/csw/trombone/hcd/JTromboneHcdHandlers.java) { #onGoOnline-handler }

### Handling commands

#### validateCommand

On receiving a command a component developer is required to validate the `ControlCommand` received. If the command is valid to be processed, `Accepted` response
should be returned from this method. Otherwise, `Invalid` response should be returned.

Assembly/Scala
:   @@snip [TromboneAssemblyHandlers.scala](../../../csw-vslice/src/main/scala/csw/trombone/assembly/TromboneAssemblyHandlers.scala) { #validateCommand-handler }

Assembly/Java
:   @@snip [JTromboneAssemblyHandlers.java](../../../csw-vslice/src/main/java/csw/trombone/assembly/JTromboneAssemblyHandlers.java) { #validateCommand-handler }

Hcd/Scala
:   @@snip [TromboneHcdHandlers.scala](../../../csw-vslice/src/main/scala/csw/trombone/hcd/TromboneHcdHandlers.scala) { #validateCommand-handler }

Hcd/Java
:   @@snip [JTromboneHcdHandlers.java](../../../csw-vslice/src/main/java/csw/trombone/hcd/JTromboneHcdHandlers.java) { #validateCommand-handler }
 
If a response can be provided immediately, a final `CommandResponse` such as `CommandCompleted` or `Error` can be sent from this handler.

#### onSubmit

In case a command is received as a submit command, command response should be updated in the `CommandResponseManager`. `CommandResponseManager` is an actor whose reference 
`commandResponseManager` is available in the `ComponentHandlers` 

#### onOneway

In case a command is received as a oneway command, command response should not be provided to the sender. 

### Tracking Connections

The component framework tracks the set of connections specified for a component in `ComponentInfo`.
The framework also provides a helper `trackConnection` method to track any connection other than those present in `ComponentInfo`.

#### onLocationTrackingEvent
The `onLocationTrackingEvent` handler can be used to take action on the `TrackingEvent` for a particular connection. 

Assembly/Scala
:   @@snip [TromboneAssemblyHandlers.scala](../../../csw-vslice/src/main/scala/csw/trombone/assembly/TromboneAssemblyHandlers.scala) { #onLocationTrackingEvent-handler }

Assembly/Java
:   @@snip [JTromboneAssemblyHandlers.java](../../../csw-vslice/src/main/java/csw/trombone/assembly/JTromboneAssemblyHandlers.java) { #onLocationTrackingEvent-handler }

Hcd/Scala
:   @@snip [TromboneHcdHandlers.scala](../../../csw-vslice/src/main/scala/csw/trombone/hcd/TromboneHcdHandlers.scala) { #onLocationTrackingEvent-handler }

Hcd/Java
:   @@snip [JTromboneHcdHandlers.java](../../../csw-vslice/src/main/java/csw/trombone/hcd/JTromboneHcdHandlers.java) { #onLocationTrackingEvent-handler }

### Publishing State
A component has access to an actor `pubSubRef` which can be used to publish its `CurrentState`. Any subscriber of this component will receive the 
published state.

## Container for deployment
A container is a component which starts one or more Components and keeps track of the components within a single JVM process. When started, the container also registers itself with the Location Service.
The components to be hosted by the container is defined using a `ContainerInfo` model which has a set of ComponentInfo objects. It is usually described as a configuration file but can also be created programmatically.
SampleContainerInfo
:   @@@vars
    ```
    name = "Sample_Container"
    components: [
      {
        name = "SampleAssembly"
        componentType = assembly
        behaviorFactoryClassName = package.component.SampleAssembly
        prefix = abc.sample.prefix
        locationServiceUsage = RegisterAndTrackServices
        connections = [
          {
            name: Sample_Hcd_1
            componentType: hcd
            connectionType: akka
          },
          {
            name: Sample_Hcd_2
            componentType: hcd
            connectionType: akka
          },
          {
            name: Sample_Hcd_3
            componentType: hcd
            connectionType: akka
          }
        ]
      },
      {
        name = "Sample_Hcd_1"
        componentType = hcd
        behaviorFactoryClassName = package.component.SampleHcd
        prefix = abc.sample.prefix
        locationServiceUsage = RegisterOnly
      },
      {
        name = "Sample_Hcd_2"
        componentType: hcd
        behaviorFactoryClassName: package.component.SampleHcd
        prefix: abc.sample.prefix
        locationServiceUsage = RegisterOnly
      }
    ]
    ```
    @@@
## Standalone components
A component can be run alone in a Standalone mode without sharing it's jvm space with any other component. 


