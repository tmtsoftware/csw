# Multiple Components

Deploying multiple components (in Container), and having them communicate with each other. This tutorial discusses constructing an Assembly.

## Creating an Assembly

To create an assembly, the component developer need to implement the `ComponentHandlers`. More details about implementing ComponentHandlers can be referred @ref:[here](./create-component.md#handlers). 

## Component Configuration (ComponentInfo)

Component configuration contains details needed to spawn a component. This configuration resides in a configuration file
for a particular component. The sample for Assembly is as follows:

```
{
    name = "Laser"
    componentType = assembly
    behaviorFactoryClassName = csw.common.components.framework.SampleComponentBehaviorFactory
    prefix = tcs.mobie.blue.filter
    locationServiceUsage = RegisterAndTrackServices
    connections = [
      {
        name: Motion_Controller
        componentType: hcd
        connectionType: akka
      },
      {
        name: Galil
        componentType: hcd
        connectionType: akka
      },
      {
        name: Eton
        componentType: hcd
        connectionType: akka
      }
    ]
}
```

@@@ note { title=Note }

The Assembly has multiple `connections` of HCDs that it will track in it's life span.

@@@

More details about each configuration and it's significance can be referred @ref:[here](./create-component.md#component-configuration-(componentinfo)).  

## Tracking Dependencies

The connections that are defined in configuration file for an assembly will be tracked by `csw-framework`. For each connection the following details are configured:

```
{
  name: Eton
  componentType: hcd
  connectionType: akka
}
``` 

The name of the component, the type(hcd, service, etc) and the connection(akka, http, tcp) will be used to create a `Connection` object. Connection object will be then used to
track the location of a component using location service.

The `Location` object represents one of the following types:

-   AkkaLocation: Represents a remote address of the actorRef. The actorRef will be the Supervisor actor of a component.
-   HttpLocation: Represents a Http URI of the web server e.g. configuration service
-   TcpLocation: Represents a tcp URI of the server e.g. event service 

More details about tracking a component using location service can be referred @ref:[here](./services/location.md#tracking-and-subscribing).
 
### onLocationTrackingEvent Handler

For all the tracked connections, whenever a location is changed, one of the following events is generated:

-   LocationUpdated: Represents if a location is added or changed
-   LocationRemoved: Represents if a location is no longer available on the network

Whenever such an event is generated, the Top level actor will call the `onLocationTrackingEvent` hook of `ComponentHandlers` with the event(LocationUpdated or LocationRemoved)
as parameter of the hook.

More details around tracking a connection can be referred @ref:[here](./framework/tracking-connections.md)

### trackConnection

If the component developer wants to track a connection that is not configured in it's configuration file then it can use the `trackConnection` method provided by `csw-framework`
in `ComponentHandlers`. The `trackConnection` method will take the `Connection` instance. How to create a connection instance can be referred @ref:[here](./services/location.md#creating-components-connections-and-registrations).

@@@ note { title=Note }

Connections tracked by `csw-framework` (from configuration file) or by component developer using `trackConnection` method both will be received in `onLocationTrackingEvent`
hook of `ComponentHandlers`. 

@@@

## Sending Commands

If a component wants to send command to other component, it may use the `CommandService` instance. The creation of a`CommandService` instance and its usage can be referred
@ref:[here](./command.md#commandservice).

If a component wants to send multiple commands in response to a single received command, then it may use `CommandDistributor` instance. The CommandDistributor will help in
getting the aggregated response of multiple commands sent to other components. The component developer can use aggregated response to update the `CommandResponseManager` with
appropriate status if the received command was in `Submit` wrapper.

More details about creating `CommandDistributor` and it's usage can be referred @ref:[here](./command.md#distributing-commands).

@@@ note { title=Note }

`CommandDistributor` can be used to get aggregated response only if the multiple commands sent to other components are all wrapped in `Submit` wrapper.

@@@

### Tracking Long Running Commands

A command, when sent in `Submit` wrapper, and received an `Accepted` response in return, is considered as a long running command.
  
When a component sends a long running command to another component, it may be interested in knowing the status of the command and take decisions based on that. In order to subscribe
to the changes in command status, the sender component will have to use `subscribe` method after `submit` or use `submitAndSubscribe` in `CommandService`.

### Matchers

When a component sends a command in `Oneway` to another component, it may be interested in knowing the receiver component's `CurrentState` and match it against a desired state.
In order to do that, the component developer can use `onewayAndMatch` method of `CommandService` or `oneway` of `CommandService` and the use `Matcher` explicitly to match a desired
state with current state.

More details on how to use `Matcher` can  be referred @ref:[here](./command.md#matching-state-for-command-completion). 

### PubSub Connection

A component might need to subscribe to current state of any other component provided it knows the location of that component. In order to subscribe to current state, it may
use `subscribeCurrentState` method of `CommandService`. More details about the usage of `subscribeCurrentState` can ber referred @ref:[here](./command.md#subscribecurrentstate).

If a component wants to publish it's current state then it can use the `currentStatePublisher` provided by `csw-framework` in `ComponentHandlers`. More details about the usage
of `currentStatePublisher` can ber referred @ref:[here](./framework/publishing-state.md).

## Deploying Multiple Components in a Container

## Running components in a Container


NOTE: the following was moved from the getting starting page. I thought it was more appropriate here, 
but I just cut/paste for now, expecting some of this info will be integrated into this page. -- JLW
## Deploying and Running Components

### Pre-requisite

`galil-deploy` project contains applications (ContainerCmd and HostConfig) to run your components, make sure you add necessary dependencies in `galil-deploy` project.


### Run
As seen above `galil-deploy` depends on `galil-assembly` and `galil-hcd`, now if you want to start these Assembly and HCD, follow below steps:

 - Run `sbt galil-deploy/universal:packageBin`, this will create self contained zip in `galil-deploy/target/universal` directory
 - Unzip generated zip file and enter into `bin` directory
 - You will see four scripts in `bin` directory (two bash scripts and two windows scripts)
 - If you want to start multiple containers on a host machine, follow this guide @ref:[here](apps/hostconfig.md#examples)
 - If you want to start multiple components in container mode or single component in standalone mode, follow this guide @ref:[here](framework/deploying-components.md)
 - Example to run container:    `./galil-container-cmd-app --local ../../../../galil-deploy/src/main/resources/GalilAssemblyContainer.conf`
 - Example to run host config:  `./galil-host-config-app --local ../../../../galil-deploy/src/main/resources/GalilHostConfig.conf -s ./galil-container-cmd-app`

@@@ note { title=Note }

CSW Location Service cluster seed must be running, and appropriate environment variables set to run apps.
See https://tmtsoftware.github.io/csw-prod/apps/cswclusterseed.html.

@@@
