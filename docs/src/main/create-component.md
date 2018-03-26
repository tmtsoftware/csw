# Creating a Component

This tutorial helps in creating a HCD in Scala/Java. In order to create a HCD one needs to depend on `csw-framework`
which can be referred [here](https://tmtsoftware.github.io/csw-prod/framework.html). This tutorial can be referred for creating an Assembly as well.

## Anatomy of Component
    
A component consists of a supervisor actor, a top level actor, a component handler and one or more worker actors. From all these, `csw-framework`
provides supervisor actor, a top level actor and abstract class of handlers. Component developers are expected to implement this handler which also
acts as a gateway from framework to component code.   
     
### Supervisor

A Supervisor actor is the actor first started for any component. Two main responsibilities that supervisor performs is as follows:

-   Spawn a top level actor for the component and start watching it
-   Register itself with location service

@@@ note { title=Note }

Supervisor registers itself with location service. That means supervisor acts as a gateway for external component/entity to talk to this component.

@@@

The source code of supervisor actor can be referred [here](https://github.com/tmtsoftware/csw-prod/blob/master/csw-framework/src/main/scala/csw/framework/internal/supervisor/SupervisorBehavior.scala)

### Top level actor

A top level actor is started by supervisor actor for any component. It takes a handler implementation (an instance of ComponentHandlers) as constructor parameter.
The handler implementation would be written by component developer.

Whenever a message is received by top level actor, it calls an appropriate method of handlers which we refer as hooks in further explanation. For e.g.
if top level actor receives `Initialize` message it will call `initialize()` hook of handlers.

The source code of top level actor can be referred [here](https://github.com/tmtsoftware/csw-prod/blob/master/csw-framework/src/main/scala/csw/framework/internal/component/ComponentBehavior.scala).

### Handlers

A `ComponentHandlers` is an abstract class provided by `csw-framework`. It provides a list of methods that a component developer should implement:

-   initialize
-   validateCommand
-   onSubmit
-   onOneway
-   onGoOffline
-   onGoOnline
-   onLocationTrackingEvent
-   trackConnection
-   onShutdown

The source code of `ComponentHandlers` can be referred [here](https://github.com/tmtsoftware/csw-prod/blob/master/csw-framework/src/main/scala/csw/framework/scaladsl/ComponentHandlers.scala). 

More details about handler significance and invocation can be referred [here](https://tmtsoftware.github.io/csw-prod/framework/handling-lifecycle.html)

@@@ note { title=Note }

If the component developer wishes to write the handler implementation in java, then he/she needs to implement the java version of `ComponentHandlers`
which is `JComponentHandlers`. The source code of `JComponentHandlers` can be referred [here](https://github.com/tmtsoftware/csw-prod/blob/master/csw-framework/src/main/scala/csw/framework/javadsl/JComponentHandlers.scala).
Any further reference to `ComponentHandlers` should implicitly also apply to `JComponentHandlers`.

@@@

## Constructing the Component

After writing the handlers, component developer needs to wire it up with framework. In order to do this, developer 
needs to implement a `ComponentBehaviorFactory`. This factory should to be configured in configuration file for
the component. The sample of configuration file is discussed next. The `csw-framework` then picks up the full path of
`ComponentBehaviorFactory` from configuration file and spawn the component handlers using this factory as a process of
booting a component. The factory is instantiated using java reflection.

The sample code to implement the `ComponentBehaviorFactory` can be referred [here](https://tmtsoftware.github.io/csw-prod/framework/creating-components.html) 

### Component Configuration (ComponentInfo)

Component configuration contains details needed to spawn a component. This configuration resides in a configuration file
for a particular component. The sample for HCD is as follows:

```
{
    name = "Motion_Controller"
    componentType = hcd
    behaviorFactoryClassName = tcs.hcd.SampleComponentBehaviorFactory
    prefix = tcs.mobie.blue.filter
    locationServiceUsage = RegisterOnly
}
``` 

@@@ note { title=Note }

`behaviorFactoryClassName` refers to class name of the concrete implementation of `ComponentBehaviorFactory`, which is `SampleComponentBehaviorFactory` in above sample.

@@@

The `name` and `componentType` is used to create the `ComponentId` representing a unique component in location service.

The `locationServiceUsage` is referred by supervisor actor to decide whether to only register a component with location service or register and track other components.
  
The configuration file is parsed to `ComponentInfo` and injected in supervisor actor. It is then injected in `ComponentHandlers` while spawning a component.

More details about `ComponentInfo` can be referred [here](https://tmtsoftware.github.io/csw-prod/framework/describing-components.html).

A sample configuration file can be referred [here](https://github.com/tmtsoftware/csw-prod/blob/master/csw-benchmark/src/main/resources/container.conf).

## Lifecycle 

A component can be in one of the following states of lifecycle:

-   Idle
-   Running
-   RunningOffline
-   Restart
-   Shutdown
-   Lock

### Idle

The component initializes in idle state. Top level actor calls the `initialize` hook of `ComponentHandlers` as first thing of boot-up.
Component developers write their initialization logic in this hook. The logic could also be like accessing the configuration service
and fetch the hardware configurations to set the hardware to default positions.

After the initialization, if the component would have configured `RegisterAndTrack` for `locationServiceUsage` then the top level actor will start tracking
the `connections` configured for that component. This use case is mostly applicable for Sequencers and Assemblies. HCDs mostly will have `RegisterOnly`
configured for `locationServiceUsage`.

Supervisor actor will now register itself with location service and transition to `Running` state. Registering with location service will notify other components
tracking this component of `LocationUpdated` event with actorRef of supervisor actor.

Other components can now start sending commands to this component.      
 
### Running

When the supervisor actor receives `Initialized` message from top level actor after successful initialization, it registers itself with location service and transits
the component to `Running` state. Running state signifies that the component is accessible via location service. Any commands received by supervisor actor will be
forwarded to top level actor. Top level actor will then call `validateCommand` hook of `ComponentHandlers` where component developers are expected to write validation
logic for the received command.

More details around receiving command is provided in subsequent sections.

### RunningOffline

When the supervisor actor receives `GoOffline` message it transits the component to `RunningOffline` state and forward it to top level actor. Top level actor then calls
`onGoOffline` hook of `ComponentHandlers`.

If `GoOnline` message is received by supervisor actor then it transits the component back to `Running` state and forward it to top level actor. Top level actor then calls
`onGoOnline` hook of `ComponentHandlkers`.

@@@ note { title=Note }

In `RunningOffline` state if any command is received then it is forwarded to underlying component hook through top level actor. It is then the responsibility of
component developer to check the `isOnline` flag provided by `csw-framework` in the logic and process the command accordingly.  

@@@

### Restart

When the supervisor actor receives a `Restart` message, it will transit the component to `Restart` state. Then it will unregister itself from location service so that other components
tracking this component will be notified and no commands are received while restart is in progress.

Then the top level actor is stopped and postStop hook of top level actor will call the `onShutdown` hook of `ComponentHandlers`. Component developers are expected to write 
any cleanup of resources or logic that should be executed for graceful shutdown of component in this hook.  

After successful shutdown of component, supervisor actor will create the top level actor all together. This will cause the `initialize` hook of `ComponentHandlers` to be called
again. After successful initialization of component, supervisor actor will register itself with location service.

### Shutdown

When the supervisor actor receives a `Shutdown` message, it transit the component to `Shutdown` state so that any commands received while shutdown is in progress will be ignored.
Then it stop will the top level actor. The postStop hook of top level actor will call the `onShutdown` hook of `ComponentHandlers`. Component developers are expected to write 
any cleanup of resources or logic that should be executed for graceful shutdown of component in this hook.

### Lock

When the supervisor actor receives a `Lock` message, it transit the component to `Lock` state. Post locking, supervisor will only forward the commands received from the component
that locked this component and ignore others.

In `Lock` state messages like `Shutdown` and `Restart` will be ignored. So, if these messages need to be send to the component, then it has to be first unlocked.

## Logging

`csw-framework` will provide a `LoggerFactory` as dependency injection in constructor of `ComponentHandlers`. The `LoggerFactory` will have the component's name predefined in
it. The component developer is expected to use this factory to log statements.

More details on how to use `LoggerFactory` can be referred [here](https://tmtsoftware.github.io/csw-prod/services/logging.html#enable-component-logging). 

## Receiving Commands

A command is something that carries some metadata and a set of parameters. A component sends message to other components using `commands`.
Various kinds of commands are as follows:

-   Setup : A sequencer or an assembly will send a setup kind of command
-   Observe: A Sequencer or an assembly will send a observe kind of command
-   Wait: A Sequencer can receive a wait kind of command

More details about creating commands can be referred [here](https://tmtsoftware.github.io/csw-prod/messages/commands.html#setup-command).

Whenever a command is sent to a component it is wrapped inside a command wrapper. There are two kinds of command wrapper:

-   Submit: A command is wrapped in submit when the completion result is expected from receiver component 
-   Oneway: A command is wrapped in oneway when the completion of command is not expected from receiver component but is determined by sender component by subscribing to receiver component's
            state

### Validation

When a command is received by a component, top level actor will call the `validateCommand` hook of `ComponentHandlers`. Component developers are expected to perform appropriate
validation of command, whether it is valid to execute, and return a `CommandResponse`. The `CommandResponse` returned from this hook will be sent back to sender directly by `csw-framework`.

The logic in `validateCommand` hook can determine whether the command received will be executed shortly or will it take longer. If the command could be executed shortly, then
component developer can return a final response directly or if the command will take longer to execute, then component developer can return an intermediate response `Accepted`
or `Invalid` specifying whether the command is valid to be executed or not.

Whether the command execution will take longer or not is subjective to each component. 

Different types of command responses and their significance can be referred [here](https://tmtsoftware.github.io/csw-prod/command.html#command-based-communication-between-components).

### Command Response

The response returned from `validateCommand` hook of `ComponentHandlers` will be received by top level actor. Top level actor then sends response back to sender. Next, if the
response returned was `Accepted` then, it either calls `onSubmit` hook or `onOneway` hook of `ComponentHandlers` depending on the wrapper(submit or oneway) in which the command
is received. 

If the command is received in submit kind of a wrapper, then top level actor adds the response returned from `validateCommand` hook in `CommandResponseManager` and then it checks
if the response was `Accepted` to call the `onSubmit` hook of `ComponentHandlers`.  

In case the command received by a component is in oneway kind of wrapper then, the response returned from `validateCommand` hook will be sent directly to sender and the `onOneway`
hook of `ComponentHandlers` will be called.

The `CommandResponseManager` is responsible for managing and bookkeeping the command status of the command received in submit kind of wrapper by a component. The sender of the
command can query the status or subscribe to changes in status using `CommandService`. `CommandService` provides helper methods for communicating with other components.

Creation of `CommandService` instance and it's usage can be referred [here](https://tmtsoftware.github.io/csw-prod/command.html#commandservice).

When `onSubmit` hook is called, it is the responsibility of component developers to update the status of received command in `CommandResponseManager` as it changes. The instance
of commandResponseManager is provided in `ComponentHandlers` which should be injected in any worker actor or other actor/class created for the component.   

More details on methods available in `CommandResponseManager` can be referred [here](https://tmtsoftware.github.io/csw-prod/framework/managing-command-state.html).

## Building and Running component in standalone mode

Once the component is ready, it need to start `ContainerCmd`. The details about starting the `ContainerCmd` in standalone mode can be referred [here](https://tmtsoftware.github.io/csw-prod/framework/deploying-components.html).

Next, to run the component refer the following steps:

-   Run `sbt <project>/universal:packageBin`. This will create self contained zip in `<project>/target/universal` directory
-   Unzip generated zip file and enter into bin directory
-   Run the `./<project>-cmd-app --local --standalone <path-to-local-config-file-to-start-the-component>`

@@@ note { title=Note }

CSW Location Service cluster seed must be running, and appropriate environment variables set to run apps.
See https://tmtsoftware.github.io/csw-prod/apps/cswclusterseed.html.

@@@

