# Multiple Components

In this part of the tutorial, we will demonstrate functionality involving multiple components.  
We will do this by creating an Assembly, demonstrating how to deploy the Assembly and an HCD in a container, and 
having them communicate with each other.

## Creating an Assembly

Similar to the HCD in the previous page, to create an Assembly, the component developer needs to implement the `ComponentHandlers`
for the Assembly.
More details about implementing ComponentHandlers can be found @ref:[here](./create-component.md#handlers). 

#### *Tutorial: Developing an Assembly*

If using the giter8 template with the default parameters, our `ComponentHandlers` class will be the `SampleHandlers` class (`JSampleHandlers` in Java),
and the factory will be `SampleBehaviorFactory` (`JSampleBehaviorFactory` in Java).

Like we did for the HCD, let's add some log messages for the `initialize` and `onShutdown` hooks, but not the 
`onTrackingLocationEvent` hook.  We'll cover that in more detail later.

Scala
:   @@snip [SampleHandlers.scala](../../../../examples/src/main/scala/org/tmt/csw/sample/SampleHandlers.scala) { #initialize }

Java
:   @@snip [JSampleHandlers.java](../../../../examples/src/main/java/org/tmt/csw/sample/JSampleHandlers.java) { #initialize }

Once again, ignore the code about setting up the event subscription. 
This will be covered later when we discuss subscribing to events.

## Component Configuration (ComponentInfo)

Also as with the HCD, we need to create a ComponentInfo file for the Assembly. The following shows an example of 
ComponentInfo file for an Assembly:

Scala
:    
```
componentType = assembly
behaviorFactoryClassName = "org.tmt.csw.sample.SampleBehaviorFactory"
prefix = "csw.sample"
locationServiceUsage = RegisterAndTrackServices
connections = [
  {
        prefix: "csw.samplehcd"
        componentType: hcd
        connectionType: akka
  }
]
```

Java
:    
```
componentType = assembly
behaviorFactoryClassName = "org.tmt.csw.sample.JSampleBehaviorFactory"
prefix = "csw.sample"
locationServiceUsage = RegisterAndTrackServices
connections = [
  {
        prefix: "csw.samplehcd"
        componentType: hcd
        connectionType: akka
  }
]
```

@@@ note { title="Connections in ComponentInfo" }

There is a section for listing connections. These are the connections that the component will 
automatically track and can be other components or services.
For instance, an Assembly may command one or more HCDs. Entries here can be used to track those 
HCDs and the Assembly will be notified if one of the tracked HCDs
starts up, crashes, or shuts down.
In some cases, it may make sense to track services such as the Event Service. 
These connections can also be specified for HCDs, but HCDs should not have any component dependencies.

@@@

The above shows a configuration file for running in standalone mode.  If we want to run both the assembly and HCD in a container, the 
ComponentInfo file combined entries for both components and looks like this:


Scala
:   @@snip [SampleContainer.conf](../../../../examples/src/main/resources/SampleContainer.conf)

Java
:   @@snip [JSampleContainer.conf](../../../../examples/src/main/resources/JSampleContainer.conf)


More details about each configuration and its significance can be found @ref:[here](./create-component.md#component-configuration-componentinfo-).

Another sample container configuration file can be found [here]($github.base_url$/csw-benchmark/src/main/resources/container.conf).  

## Tracking Dependencies

The connections that are defined in the ComponentInfo file for an Assembly will be tracked by the `csw-framework`. 
For each connection the following details are configured:

Scala
:    
```
{
    prefix: "csw.samplehcd"
    componentType: hcd
    connectionType: akka
}
``` 

Java
:    
```
{
    prefix: "csw.samplehcd"
    componentType: hcd
    connectionType: akka
}
``` 

The configuration includes the `prefix` of the component consisting of a valid subsystem and the component's name. 
The prefix, component type (hcd, service, etc), and the connection type (akka, http, tcp) will be used to create a `Connection` object. 
The Connection object will be then used to track the location of a component using Location Service.

The `Location` object has one of the following types:

-   AkkaLocation: Contains the remote address of the actorRef. The actorRef will be the Supervisor actor of a component.
-   HttpLocation: Holds the HTTP URI of the web server, e.g. Configuration Service
-   TcpLocation: Represents a TCP URI of the server or service, e.g. Event Service 

More details about tracking a component using the Location Service can be found @ref:[here](../services/location.md#tracking-and-subscribing).
 
## onLocationTrackingEvent Handler

For all the tracked connections, whenever a `Location` is changed, added, or removed, one of the following events is generated:

-   LocationUpdated: a location was added or changed
-   LocationRemoved: a location is no longer available on the network

Whenever such an event is generated, the Top level actor will call the `onLocationTrackingEvent` hook of `ComponentHandlers` with the event (LocationUpdated or LocationRemoved)
as parameter of the handler.

More details about tracking connections can be found @ref:[here](../framework/tracking-connections.md).

#### *Tutorial: Developing an Assembly*

For our sample component, we will set it up so that when the HCD is found by the Location Service, we will immediately send a command to it.  We
will do this by using the `Location` obtained to create a `CommandService` reference (see @ref:[below](multiple-components.md#sending-commands)) to the HCD
and store at as a variable in the TLA. This can be a good way to easily track the connection and keep a reference up to date.
The code then sends a `shortCommand` command through the onSetup handler. Sending commands from the location event tracking handler is not a best practice; it
is shown here for demonstration purposes.

Similarly, if the HCD is removed, a message is logged and the Command Service variable is set to none.  

Scala
:   @@snip [SampleHandlers.scala](../../../../examples/src/main/scala/org/tmt/csw/sample/SampleHandlers.scala) { #track-location }

Java
:   @@snip [JSampleHandlers.java](../../../../examples/src/main/java/org/tmt/csw/sample/JSampleHandlers.java) { #track-location }



## trackConnection

If the component developer wants to track a connection that is not configured in its configuration file then it 
can use the `trackConnection` method provided by `csw-framework`
in `ComponentHandlers`. The `trackConnection` method will take the `Connection` instance. 
Information on how to create a connection instance can be found @ref:[here](../services/location.md#creating-components-connections-and-registrations).

@@@ note { title=Note }

Connections tracked by `csw-framework` (from the configuration file) or by a component developer using the `trackConnection` method both will be received in the `onLocationTrackingEvent`
hook of `ComponentHandlers`. 

@@@

## Sending Commands

From the location information obtained either by tracking dependencies or manually resolving a location, a `CommandService` instance
can be created to provide a command interface to the component.  The following snippet, not from our tutorial, shows how
 to obtain a `CommandService` reference using by resolving a location using the Location Service.

Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/example/framework/components/assembly/AssemblyComponentHandlers.scala) { #resolve-hcd-and-create-commandservice }

Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/example/framework/components/assembly/JAssemblyComponentHandlers.java) { #resolve-hcd-and-create-commandservice }


If a component wants to send a command to another component, it uses a `CommandService` instance. The creation of a`CommandService` instance and its usage can be found
@ref:[here](./command.md#commandservice).


## Handling Short and Long-Running Commands

The Command Service provides a two different ways of performing `Submit` commands.   In particular, for sending single commands, there are two flavors: `submit`
and `submitAndWait`.  Both commands take a `Setup` or `Observe` and return a Future response encapsulating a `SubmitResponse` type.  

To understand the difference between `submit` and `submitAndWait` and how to choose between them it is necessary to understand
how commands are processed in the destination component, and what you as the developer need to accomplish.
Every submitted command is first validated by calling the `validateCommand` handler. 
If `validateCommand` returns `Invalid`, the submit returns immediately. If `validateCommand` returns `Accepted`, the
`onSubmit` handler is called. 

If a command is short, less than 1 second, it completes quickly with the response of `Completed ` or an `Error` response. In
this case `submit` and `submitAndWait` behave the same working like a function call or RPC command. The `Completed` response can also return a result if needed.
 
If the command is long-running, longer than 1 second, an `onSubmit` handler should start the actions and return a `Started` response. The 
sender of the command obtains the final response of the command by polling using the Command Service `query` call or by waiting using `queryFinal`.
When an `onSubmit` handler returns `Started` it is expected to update the command with the final response when the actions complete 
using the `updateCommand` call of the `CommandResponseManager` that is provided to the TLA in the `CswContext`.

On the sender side, `submitAndWait` doesn't complete until the long-running command completes and returns the final 
`SubmitResponse`.  That is to say, if the Future completes, 
no matter whether the command has an immediate response or is long-running, is invalid, or encounters an error at any point, 
the Future completes with the final response.  If there is an exception, which may occur if the command times out, that must
be handled explicitly using Future exception handling (this can be done many ways; an example is given below).

#### *Tutorial: Developing an Assembly*

TWe use our worker actor to submit the command to the HCD, and then subscribe to the HCD's `CommandResponseManager` for command completion, using the shortcut `submitAndWait`.
 
Scala
:   @@snip [SampleHandlers.scala](../../../../examples/src/main/scala/org/tmt/csw/sample/SampleHandlers.scala) { #worker-actor }
 
Java
:   @@snip [SampleHandlers.java](../../../../examples/src/main/java/org/tmt/csw/sample/JSampleHandlers.java) { #worker-actor }

@@@ note { title="Waiting is not Waiting" }

Even through `submitAndWait` waits, it is not blocking. The Assembly and HCD are still available for commands. 
The support for commands is all asynchronous. For example, `complexCommand` sends two commands to the HCD, which is happy to 
execute two sleeps concurrently. The Assembly is also available for other commands. For instance, other versions
of the SampleAssembly show how to cancel a long-running command. 

Note also that if your Assembly or HCD can only execute one command at a time, your code must test for this during
validation and stop multiple commands from executing.

@@@


## Matchers

When a component sends a command as `Oneway` to another component, it may be interested in knowing the receiver component's `CurrentState` and match it against a desired state.
In order to do that, the component developer can use the `onewayAndMatch` method of `CommandService` or use `oneway` and then use a `Matcher` explicitly to match a desired
state with current state.

More details on how to use `Matcher` can  be found @ref:[here](./command.md#matching-state-for-command-completion). 

## PubSub Connection

A component might need to subscribe to the current state of any other component provided it knows the location of that 
component. In order to subscribe to current state, it may
use the `subscribeCurrentState` method of the `CommandService`. More details about the usage 
of `subscribeCurrentState` can be found @ref:[here](./command.md#subscribecurrentstate).

If a component wants to publish its current state then it can use the `currentStatePublisher` provided by `csw-framework`
in the `CswContext` object passed into `ComponentHandlers`. More details about the usage
of `currentStatePublisher` can be found @ref:[here](../framework/publishing-state.md).

## Subscribing to Events

To subscribe to events, a subscriber is accessed in a similar way to publishing.  Typically a `defaultSubscriber`
is obtained, but additional subscribers with their own connection can be created.

The subscribe API specifies a set of `Events` to subscribe to and then specifies how the events should be handled. 
This can be a callback, an Actor reference to receive the `Event` as a message, or as a stream to allow flow operations
to be applied. 

#### *Tutorial: Developing an Assembly*

We will setup our subscription to the counter events generated by our HCD in the `subscribeToHCD` method.  

Scala
:   @@snip [SampleHandlers.scala](../../../../examples/src/main/scala/org/tmt/csw/sample/SampleHandlers.scala) { #subscribe }

Java
:   @@snip [SampleHandlers.java](../../../../examples/src/main/java/org/tmt/csw/sample/JSampleHandlers.java) { #subscribe }


We use the `subscribeCallback` method from the API and specify the method `processEvent` as our callback, in which we 
unpack the event and log the counter value.  The subscribe methods in the API return a `EventSubscription` object, which can 
be used to stop the subscription, as demonstrated in the `unsubscribeHCD` method (which again, is not called in our tutorial).

Again, we return to our `initialize` method to show how subscription is started, and the reference to the subscription is stored
for later use.

Scala
:   @@snip [SampleHandlers.scala](../../../../examples/src/main/scala/org/tmt/csw/sample/SampleHandlers.scala) { #initialize }

Java
:   @@snip [SampleHandlers.java](../../../../examples/src/main/java/org/tmt/csw/sample/JSampleHandlers.java) { #initialize }


## Deploying and Running Components

### Pre-requisite

A project, for example with the name `sample-deploy`, contains applications (ContainerCmd and HostConfig coming from `csw-framework`) to run components. Make sure that the necessary 
dependencies are added in the `sample-deploy`.

### Run
To start the Assembly and HCD, `sbt runMain` can be used as with the HCD, but with slightly different options.  
Now, we do not want to run in standalone mode, and we need to make sure to pass the container configuration file.

Go to the project root directory and type `sbt "<deploy-module>/runMain <mainClass> --local <path-to-config-file>"`, where
 
- `<deploy-module>` is the name of the deployment module created by the template (`sample-deploy` if using defaults) 
- `<mainClass>` is the full class name of our ContainerCmd application, which the template names `<package>.<name>deploy.<Name>ContainerCmdApp`.
If you accept the defaults for the template, it will be `org.tmt.csw.sampledeploy.SampleContainerCmdApp`.  If you are having problems
determining the class name, use `sbt <deploy-module>/run` and it will prompt you the possibilities.
- `<path-to-config-file>` is the filename, which can be an absolute path or relative to the directory of the deployment module.  If using defaults,
this would be `src/main/resources/SampleContainer.conf` for Scala, and `src/main/resources/JSampleContainer.conf` for Java.

So if using the template defaults, the full command would be:

Scala
:    
```
sbt "sample-deploy/runMain org.tmt.csw.sampledeploy.SampleContainerCmdApp --local src/main/resources/SampleContainer.conf"
```

Java
:    
```
sbt "sample-deploy/runMain org.tmt.csw.sampledeploy.SampleContainerCmdApp --local src/main/resources/JSampleContainer.conf"
```

@@@ note { title=Note }

This assumes you still have the CSW Services running using the  `csw-services` application as described in the 
@ref:[Create a Component](./create-component.md#starting-csw-services) tutorial page.
@@@
