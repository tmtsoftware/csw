# Location Service

The Location Service handles component (i.e., Applications, Sequencers, Assemblies, HCDs, and Services) registration 
and discovery in the distributed TMT software system.

The CSW Location Service cluster must be running, and appropriate environment variables set to run apps.
See @ref:[CSW Location Server](../apps/cswlocationserver.md).

A component’s location information can be used by other components and services to connect to it and use it. 
An example of location information is:
 
* host address/port pairs
* URL/URIs paths
* connection protocols

## Dependencies

To use the Location Service without using the framework, add this to your `build.sbt` file:

sbt
:   @@@vars
    ```scala
    libraryDependencies += "com.github.tmtsoftware.csw" %% "csw-location-server" % "$version$"
    ```
    @@@


## Create Location Service

Note that before using this API, the [csw-location-server](../apps/cswlocationserver.html) application 
should be running at a known location in the 
network (or at multiple locations) and the necessary configuration, environment variables or system 
properties should be defined to point to the correct host and port number(s).

`LocationServiceFactory` provides a make method to create an instance of the LocationService API. 
This call will look for configuration or environment variable settings.

Scala
:   @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/example/location/LocationServiceExampleClientApp.scala) { #create-location-service }

Java
:   @@snip [JLocationServiceExampleClient.java](../../../../examples/src/main/java/example/location/JLocationServiceExampleClient.java) { #create-location-service }


## Creating Components, Connections and Registrations

An Application, Sequencer, Assembly, HCD, or Service may need to be used by another component as part of normal observatory operations. 
It must register its location information with Location Service so that other components can find it. Location information is comprised of:

* **ComponentId** : A component ID consisting of 
    * **ComponentName** : a name describing the component.
    * **ComponentType** : such as Container, Sequencer, HCD, Assembly, Service.
   
* **ConnectionType** : the means to reach components. These are categorized as `Akka`, `HTTP`, or `Tcp` type connections.

The location information is stored in the Location Service as **Registrations**.

Some of the examples of the string representation of a connection are:
 
* TromboneAssembly-assembly-akka 
* TromboneHcd-hcd-akka 
* ConfigServer-service-http 
* EventService-service-tcp

The `register` API takes a `Registration` parameter and returns a future registration result. 
If registration fails for some reason, the returned future will fail with an exception. 
(Registration will fail if the `csw-location-server` application is not running or could not be found, or if the given component name was already registered.)

`AkkaRegistrationFactory` can be used to instantiate `AkkaRegistration` using `AkkaConnection`, `Actor Ref` and optional `Metadata`.
The following example shows registration of both an UnTyped ActorRef and a Typed ActorRef:
 
Scala
:   @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/example/location/LocationServiceExampleClientApp.scala) { #Components-Connections-Registrations }

Java
:   @@snip [JLocationServiceExampleClient.java](../../../../examples/src/main/java/example/location/JLocationServiceExampleClient.java) { #Components-Connections-Registrations }



### Capability to add metadata while registering connection

Location service supports to add additional information while registering connection. This information is metadata for that registration.
Metadata given at point of registration is reflected in location model. This metadata information can be used to store additional info 
e.g. agentId for any component running on that agent. Metadata field is optional while registration, if not provided location will be reflected with empty metadata

Following example shows adding metadata in `HttpRegistration`. Similarly, metadata can be added in `AkkaRegistration` as well as `TcpRegistration`.
Once location is registered `Metadata` associated can be used for any computation.

Scala
:   @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/example/location/LocationServiceExampleClientApp.scala) { #Components-Connections-Registrations-With-Metadata }

Java
:   @@snip [JLocationServiceExampleClient.java](../../../../examples/src/main/java/example/location/JLocationServiceExampleClient.java) { #Components-Connections-Registrations-With-Metadata }


@@@ note

The `AkkaRegistration` api takes only Typed ActorRefs. Hence, to register an UnTyped ActorRef for an akka connection, it needs to be
adapted to Typed `ActorRef[Nothing]`, using adapters provided by Akka.  The usage of the adapter is
shown in the above snippet for both Scala and Java. 

Also, note that for components, the registration will be taken care of via `csw-framework`. Hence, component developers won't register any connections during their development.
So, the above demonstration of registering connections is for explanatory and testing purposes only.  
@@@

## Creating ActorRef for Registration

The ActorSystem used to start actors that will be registered in the Location Service must be created using an `ActorSystemFactory` as follows:
 

Scala
:  @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/example/location/LocationServiceExampleClientApp.scala) { #create-actor-system }

Java
:  @@snip [JLocationServiceExampleClient.java](../../../../examples/src/main/java/example/location/JLocationServiceExampleClient.java) { #create-actor-system }

This is required to start a remote ActorSystem on the same network interface where the csw-cluster is running. 
All the ActorRefs created using this ActorSystem will be available for communication from other components 
that are part of the CSW Cluster.

## Resolving Connections

The `list` API returns a list of the currently registered connections from the Location Service.
  
A connection of interest can be looked up using the `resolve` or `find` methods:   

`resolve` gets the location for a connection from the local cache. 
If not found in the cache, it waits for the event to arrive within the specified time limit and returns None on failure.    

`find` returns the location for a connection from the local cache and returns None if not immediately found there.    

Scala
:   @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/example/location/LocationServiceExampleClientApp.scala) { #find }

Java
:   @@snip [JLocationServiceExampleClient.java](../../../../examples/src/main/java/example/location/JLocationServiceExampleClient.java) { #find }

The logging output from the above example when the given component is not registered should include:

```
[INFO] Attempting to find connection AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly))
[INFO] Result of the find call: None 
```

An example of the resolve command is shown in the following: 

Scala
:   @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/example/location/LocationServiceExampleClientApp.scala) { #resolve }

Java
:   @@snip [JLocationServiceExampleClient.java](../../../../examples/src/main/java/example/location/JLocationServiceExampleClient.java) { #resolve }

The logging output from the above example should include:

```
[INFO] Attempting to resolve AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly)) with a wait of 30 seconds ...
```

If you then start the LocationServiceExampleComponentApp, the following message will be logged:
```
[INFO] Resolve result: LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType
```

If not, eventually the operation will timeout and the output should read:
```
[INFO] Timeout waiting for location AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly)) to resolve.
```

@@@ note

The `resolve` and `find` api returns the concrete `Location` type i.e. `Akkalocation`, `HttpLocation` or `TcpLocation` as demonstrated in this section. Once the Akka location
is found or resolved, we need to ascribe the type to the ActorRef, since the explicit type annotation is removed from the program before it is executed at run-time 
(see [type erasure](https://en.wikipedia.org/wiki/Type_erasure)). Use following `AkkaLocation` API to get the correct Typed ActorRef:

Scala
:   @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/example/location/LocationServiceExampleClientApp.scala) { #typed-ref }

Java
:   @@snip [JLocationServiceExampleClient.java](../../../../examples/src/main/java/example/location/JLocationServiceExampleClient.java) { #typed-ref }

@@@ 
## Filtering

The `list` API and its variants offer means to inquire about available connections with the Location Service. 
The *parameter-less* `list` returns all available connections

Scala
:   @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/example/location/LocationServiceExampleClientApp.scala) { #list }

Java
:   @@snip [JLocationServiceExampleClient.java](../../../../examples/src/main/java/example/location/JLocationServiceExampleClient.java) { #list }

The log output from the above should contain:

```
[INFO] All Registered Connections:
[INFO] --- configuration-service-http, component type=Service, connection type=HttpType
[INFO] --- hcd1-hcd-akka, component type=HCD, connection type=AkkaType
[INFO] --- LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType
[INFO] --- redis-service-tcp, component type=Service, connection type=TcpType
[INFO] --- assembly1-assembly-akka, component type=Assembly, connection type=AkkaType
```
Other variants are filters using `ConnectionType`, `ComponentType`, and `hostname`.

Filtering by component type is shown below:

Scala
:   @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/example/location/LocationServiceExampleClientApp.scala) { #filtering-component }

Java
:   @@snip [JLocationServiceExampleClient.java](../../../../examples/src/main/java/example/location/JLocationServiceExampleClient.java) { #filtering-component }

The log output from the above code should contain:

```
[INFO] Registered Assemblies:
[INFO] --- LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType
[INFO] --- assembly1-assembly-akka, component type=Assembly, connection type=AkkaType
```

Filtering by connection type is shown below:

Scala
:   @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/example/location/LocationServiceExampleClientApp.scala) { #filtering-connection }

Java
:   @@snip [JLocationServiceExampleClient.java](../../../../examples/src/main/java/example/location/JLocationServiceExampleClient.java) { #filtering-connection }

The log output should contain:

```
[INFO] Registered Akka connections:
[INFO] --- hcd1-hcd-akka, component type=HCD, connection type=AkkaType
[INFO] --- LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType
[INFO] --- assembly1-assembly-akka, component type=Assembly, connection type=AkkaType
```

Filtering akka connections by prefix is shown below:

Scala
:   @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/example/location/LocationServiceExampleClientApp.scala) { #filtering-prefix }

Java
:   @@snip [JLocationServiceExampleClient.java](../../../../examples/src/main/java/example/location/JLocationServiceExampleClient.java) { #filtering-prefix }

The log output should contain:

```
[INFO] Registered akka locations for nfiraos.ncc
[INFO] --- nfiraos.ncc.hcd1-hcd-akka, component type=HCD, connection type=AkkaType
[INFO] --- nfiraos.ncc.assembly1-assembly-akka, component type=Assembly, connection type=AkkaType
```

## Tracking and Subscribing

The lifecycle of a connection of interest can be followed using either the `track` API or the `subscribe` API.  

These methods take a `Connection` instance as a parameter. **A `Connection` need not already be registered with Location Service.** 
It's okay to track connections that will be registered in future. 

The `track` API returns two values:     
  * A **source** that will emit a stream of `TrackingEvents` for the connection.  
  * A **KillSwitch** to turn off the stream when no longer needed.  

The Akka stream API provides many building blocks to process this stream, such as Flow and Sink. 
In the example below, `Sink.actorRef` is used to forward any location messages received to the current actor (self).

A consumer can shut down the stream using the KillSwitch.

The `subscribe` API allows the caller to track a connection and receive the TrackingEvent notifications via a callback. 

The API expects following parameters:    
  * An existing connection or a connection to be registered in the future.  
  * A callback that implements `Consumer` and receives the TrackEvent as a parameter.  

@@@ note
Callbacks are not thread-safe on the JVM. If you are doing side effects/mutations inside the callback, you should ensure that it is done in a thread-safe way inside an actor. Here is an [example]($github.base_url$/examples/src/main/scala/example/event/ConcurrencyInCallbacksExample.scala) of how it can be done.
@@@

This API returns a KillSwitch that can be used to turn off the event notifications and release the supplied callback, if required.
 

Scala
:   @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/example/location/LocationServiceExampleClientApp.scala) { #tracking }

Java
:   @@snip [JLocationServiceExampleClient.java](../../../../examples/src/main/java/example/location/JLocationServiceExampleClient.java) { #tracking }

The log output should contain the following:
```
[INFO] Starting to track AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly)) LocationUpdated(HttpLocation(HttpConnection(ComponentId(configuration,Service)),http://131.215.210.170:8080/path123))
[INFO] Starting a subscription to AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly))
[INFO] Subscribing to connection LocationServiceExampleComponent-assembly-akka
[INFO] Location updated LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType
[INFO] subscription event
[INFO] Location updated LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType
```

If you now stop the LocationServiceExampleComponentApp, it would log:
```
[INFO] subscription event
[INFO] Location removed AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly))
[INFO] Location removed AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly))
```

If you start the LocationServiceExampleComponentApp again, the log output should be:
```
[INFO] subscription event
[INFO] Location updated LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType
[INFO] Location updated LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType
```

Note: the line after the words "subscription event" in our example is generated by the subscription, and the other line is from
tracking.  These two events could come in any order.


## Unregistering

One of the ways to `unregister` a service is by calling unregister on registration result received from `register` API.

Scala
:   @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/example/location/LocationServiceExampleClientApp.scala) { #unregister }

Java
:   @@snip [JLocationServiceExampleClient.java](../../../../examples/src/main/java/example/location/JLocationServiceExampleClient.java) { #unregister }

## Protected Routes
The following Location Server routes are `Protected`. To use these routes, the user must be authenticated and authorized with `location-admin` role.

* register
* unregister
* unregisterAll

@@@ note

Refer to @ref:[csw-aas](../services/aas.md) docs to know more about how to authenticate and authorize with AAS and get an access token.

@@@

## Technical Description
See @ref:[Location Service Technical Description](../technical/location/location.md).

## Source code for examples

* [Scala Example]($github.base_url$/examples/src/main/scala/example/location/LocationServiceExampleClientApp.scala)
* [JavaBlocking Example]($github.base_url$/examples/src/main/java/example/location/JLocationServiceExampleClient.java)
