# Multiple Components

In this part of the tutorial, we will demonstrate functionality involving multiple components.  
We will do this by creating an Assembly, demonstrating how to deploy start the Assembly and an HCD in a container, and 
having them communicate with each other.

## Creating an Assembly

Similar to the HCD in the previous page, to create an assembly, the component developer needs to implement the `ComponentHandlers`. More details about implementing ComponentHandlers can be found @ref:[here](./create-component.md#handlers). 

## Component Configuration (ComponentInfo)

Also similar  to the HCD, we will need to create a ComponentInfo file for the Assembly.  The following shows an example 
ComponentInfo file for an Assembly:

```
name = "GalilAssembly"
componentType = assembly
behaviorFactoryClassName = "org.tmt.nfiraos.galilassembly.GalilAssemblyBehaviorFactory"
prefix = "galil.assembly"
locationServiceUsage = RegisterAndTrackServices
connections = [
  {
    name: "GalilHcd"
    componentType: hcd
    connectionType: akka
  }
]
```
Note that there is a section for listing connections.   These are the connections that the component will automatically track, and can be other components or services.
When available, it may make sense to track things like the Event Service.  These connections can also be specified for HCDs, but of course, the should not have any component dependencies.

The above show a configuration file for running in standalone mode.  If we want to run both the assembly and HCD in a container, the file would look like this:

```
name = "GalilAssemblyContainer"
components: [
  {
    name = "GalilAssembly"
    componentType = assembly
    behaviorFactoryClassName = "org.tmt.nfiraos.galilassembly.GalilAssemblyBehaviorFactory"
    prefix = "galil.assembly"
    locationServiceUsage = RegisterAndTrackServices
    connections = [
      {
        name: "GalilHcd"
        componentType: hcd
        connectionType: akka
      }
    ]
  },
  {
    name = "GalilHcd"
    componentType = hcd
    behaviorFactoryClassName = "org.tmt.nfiraos.galilhcd.GalilHcdBehaviorFactory"
    prefix = "galil.hcd"
    locationServiceUsage = RegisterOnly
  }
]
```

More details about each configuration and its significance can be found @ref:[here](./create-component.md#component-configuration-componentinfo-).

Another sample container configuration file can be found [here](https://github.com/tmtsoftware/csw-prod/blob/master/csw-benchmark/src/main/resources/container.conf).  

## Tracking Dependencies

The connections that are defined in the configuration file for an assembly will be tracked by the `csw-framework`. For each connection the following details are configured:

```
{
    name: "GalilHcd"
    componentType: hcd
    connectionType: akka
}
``` 

The name of the component, the type(hcd, service, etc) and the connection(akka, http, tcp) will be used to create a `Connection` object. The Connection object will be then used to
track the location of a component using location service.

The `Location` object has one of the following types:

-   AkkaLocation: Contains the remote address of the actorRef. The actorRef will be the Supervisor actor of a component.
-   HttpLocation: Holds the HTTP URI of the web server, e.g. Configuration Service
-   TcpLocation: Represents a TCP URI of the server, e.g. Event Service 

More details about tracking a component using the location service can be found @ref:[here](../services/location.md#tracking-and-subscribing).
 
### onLocationTrackingEvent Handler

For all the tracked connections, whenever a location is changed, added, or removed, one of the following events is generated:

-   LocationUpdated: a location was added or changed
-   LocationRemoved: a location is no longer available on the network

Whenever such an event is generated, the Top level actor will call the `onLocationTrackingEvent` hook of `ComponentHandlers` with the event(LocationUpdated or LocationRemoved)
as parameter of the hook.

More details about tracking connections can be found @ref:[here](../framework/tracking-connections.md).

### trackConnection

If the component developer wants to track a connection that is not configured in its configuration file then it can use the `trackConnection` method provided by `csw-framework`
in `ComponentHandlers`. The `trackConnection` method will take the `Connection` instance. Information on how to create a connection instance can be found @ref:[here](../services/location.md#creating-components-connections-and-registrations).

@@@ note { title=Note }

Connections tracked by `csw-framework` (from a configuration file) or by a component developer using the `trackConnection` method both will be received in the `onLocationTrackingEvent`
hook of `ComponentHandlers`. 

@@@

## Sending Commands

From the location information obtained either by tracking dependencies or manually resolving a location, a `CommandService` instance
can be created to provide a command interface to the component.

```
implicit val actorSystem = ctx.system
val hcd = locationService.resolve(hcd.of[AkkaLocation], 5.seconds).map {
    case hcdLocation: Some(AkkaLocation) => new CommandService(hcdLocation)
    case _ => throw HcdNotFoundException()
}
```

or in Java:
```java
JCommandService hcd;
CompletableFuture<Optional<AkkaLocation>> resolve = locationService.resolve(mayBeConnection.get().<AkkaLocation>of(), FiniteDuration.apply(5, TimeUnit.SECONDS));
Optional<AkkaLocatoin> resolveHcd = resolve.get();
if(resolvedHcd.isPresent())
    hcd = new JCommandService(hcdLocation.get(), ctx.getSystem());
else
    throw new HcdNotFoundException();
```

If a component wants to send a command to another component, it uses a `CommandService` instance. The creation of a`CommandService` instance and its usage can be found
@ref:[here](./command.md#commandservice).

If a component wants to send multiple commands in response to a single received command, then it can use a `CommandDistributor` instance. The CommandDistributor will help in
getting the aggregated response of multiple commands sent to other components. The component developer can use the aggregated response to update the `CommandResponseManager` with the
appropriate status if the received command was in a `Submit` wrapper.

More details about creating a `CommandDistributor` instance and its usage can be found @ref:[here](./command.md#distributing-commands).

@@@ note { title=Note }

`CommandDistributor` can be used to get an aggregated response only if the multiple commands sent to other components are all wrapped in a `Submit` wrapper.

@@@

### Tracking Long Running Commands

A command sent in a `Submit` wrapper that receives an `Accepted` response in return is considered as a long running command.
  
When a component sends a long running command to another component, it may be interested in knowing the status of the command and take decisions based on that. In order to subscribe
to the changes in command status, the sender component will have to use the `subscribe` method after `submit` or use `submitAndSubscribe` in `CommandService`.

### Matchers

When a component sends a command as `Oneway` to another component, it may be interested in knowing the receiver component's `CurrentState` and match it against a desired state.
In order to do that, the component developer can use the `onewayAndMatch` method of `CommandService` or use `oneway` and then use a `Matcher` explicitly to match a desired
state with current state.

More details on how to use `Matcher` can  be found @ref:[here](./command.md#matching-state-for-command-completion). 

### PubSub Connection

A component might need to subscribe to the current state of any other component provided it knows the location of that component. In order to subscribe to current state, it may
use the `subscribeCurrentState` method of the `CommandService`. More details about the usage of `subscribeCurrentState` can ber found @ref:[here](./command.md#subscribecurrentstate).

If a component wants to publish its current state then it can use the `currentStatePublisher` provided by `csw-framework` in `ComponentHandlers`. More details about the usage
of `currentStatePublisher` can ber found @ref:[here](../framework/publishing-state.md).

## Deploying and Running Components

### Pre-requisite

A project, for example with the name `galil-deploy`, contains applications (ContainerCmd and HostConfig coming from `csw-framework`) to run components. Make sure that the necessary 
dependencies are added in the `galil-deploy`.

### Run
Assuming that `galil-deploy` depends on `galil-assembly` and `galil-hcd`, to start the Assembly and HCD, follow the steps below:

 - Run `sbt galil-deploy/universal:packageBin`, this will create self contained zip in `galil-deploy/target/universal` directory.
 - Unzip the generated zip file and enter into `bin` directory.
 - You will see four scripts in the `bin` directory (two bash scripts and two windows scripts).
 - If you want to start multiple containers on a host machine, follow this guide @ref:[here](../apps/hostconfig.md#examples).
 - If you want to start multiple components in container mode or single component in standalone mode, follow this guide @ref:[here](../framework/deploying-components.md).
 - Example to run container:    `./galil-container-cmd-app --local ../../../../galil-deploy/src/main/resources/GalilAssemblyContainer.conf`
 - Example to run host config:  `./galil-host-config-app --local ../../../../galil-deploy/src/main/resources/GalilHostConfig.conf -s ./galil-container-cmd-app`

@@@ note { title=Note }

The CSW Location Service cluster seed must be running and appropriate environment variables set to run the apps.
See https://tmtsoftware.github.io/csw-prod/apps/cswclusterseed.html.

@@@
