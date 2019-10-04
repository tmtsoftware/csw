# Component Handlers
A component developer creates a Top Level Actor (TLA) by inheriting from an abstract class 
@scaladoc[ComponentHandlers](csw.framework.scaladsl.ComponentHandlers) or 
@javadoc[JComponentHandlers](csw.framework.javadsl.JComponentHandlers) for Scala or Java, respectively. 
Each of these abstract classes provides several **handler** methods that can be overridden by the developer to provide
component-specific code as described below.  

## Component Lifecycle
For each component, the CSW framework creates a `Supervisor` that creates the TLA, and along with the abstract behavior
class provided by the framework, it starts up and initializes the component in a standardized way. At the conclusion of 
the startup of the component, it is ready to receive commands from the outside world. The following figure is used to 
describe the startup lifecycle interactions between the framework and the TLA.

![lifecycle](../images/framework/Lifecycle.png)


### initialize

As described in @ref:[Creating a Component](../commons/create-component.md),  a `Supervisor` is created based
on the contents of the @ref:[ComponentInfo](describing-components.md) file.  The figure shows
that the Supervisor in the framework creates the specified TLA. Once the TLA is created, the framework calls the `initialize`
handler. This is the opportunity for the component to perform any initialization needed before it is ready to receive commands.

The implementation of the `initialize` handler is up to the developer.  A common task will be for the component to fetch a configuration
from the Configuration Service.  It may also determine the location of components or services it needs from the Location Service.

The TLA indicates a successful `initialize` by returning normally. If it cannot initialize, the handler should throw an exception, which will be
caught and logged. The Supervisor will retry the creation and initialization of the TLA three times. If it fails after
three times, the Supervisor will log a message and stop.

When `initialize` succeeds, the Supervisor in the framework and the component itself enter the Running state. When in
the Running state, commands received from outside the component are passed to the TLA (see below).

Assembly/Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/example/framework/components/assembly/AssemblyComponentHandlers.scala) { #initialize-handler }

Assembly/Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/example/framework/components/assembly/JAssemblyComponentHandlers.java) { #jInitialize-handler }

Hcd/Scala
:   @@snip [HcdComponentHandlers.scala](../../../../examples/src/main/scala/example/framework/components/hcd/HcdComponentHandlers.scala) { #initialize-handler }

Hcd/Java
:   @@snip [JHcdComponentHandlers.java](../../../../examples/src/main/java/example/framework/components/hcd/JHcdComponentHandlers.java) { #jInitialize-handler }


####Creation Timeout
The `Supervisor` waits for the `initialize` to complete. If it times out, it will retry the creation of the TLA
3 times in the same way as with initialize failures. The timeout value is configurable by the TLA by setting the
`initializeTimeout` value in @ref:[ComponentInfo](describing-components.md).

####Location Service Interactions
Once the Supervisor and TLA are in the Running state, the Supervisor registers the component with the Location Service.
This allows the component to be located so it can be contacted. Registration with Location Service happens only if
locationServiceUsage in @ref:[ComponentInfo](describing-components.md) is not set to `DoNotRegister`.

If the component has connections and locationServiceUsage in @ref:[ComponentInfo](describing-components.md) is set to
`RegisterAndTrackServices`, the framework will resolve the components and deliver `TrackingEvent`s to the TLA through
the `onTrackingEvent` @ref:[`onTrackingEvent`](tracking-connections.md) handler. 


## Shutting Down
A component may be shutdown by an external administrative program whether it is deployed in a container or standalone.
Shutting down may occur when the component is in the `Running` state, either `online` or `offline` (see below).

### onShutdown

The TLA provides a handler called `onShutdown` that is called by the Supervisor when shutting down to give the TLA an opportunity to perform
any clean up it may require, such as freeing resources.

As with `initialize`, there is a timeout that the framework will wait for the component to return from `onShutdown`.  This is 
currently set to 10 seconds and cannot be overridden.
If it does not return, it is assumed that the TLA is damaged and the TLA is destroyed immediately. After a
successful return from `onShutdown`, the Supervisor deletes the component. 

Assembly/Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/example/framework/components/assembly/AssemblyComponentHandlers.scala) { #onShutdown-handler }

Assembly/Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/example/framework/components/assembly/JAssemblyComponentHandlers.java) { #onShutdown-handler }

Hcd/Scala
:   @@snip [HcdComponentHandlers.scala](../../../../examples/src/main/scala/example/framework/components/hcd/HcdComponentHandlers.scala) { #onShutdown-handler }

Hcd/Java
:   @@snip [JHcdComponentHandlers.java](../../../../examples/src/main/java/example/framework/components/hcd/JHcdComponentHandlers.java) { #onShutdown-handler }

### Restarting
A component may be restarted by an external administrative program whether it is deployed in a container or standalone.
A restart may occur when the component is in the `Running` state, either `online` or `offline` (see below).

A `restart` causes the component to be destroyed and re-created with a new TLA. The `onShutdown`
handler is called to allow the component to tidy up before it is destroyed. Then the Supervisor
creates a new TLA and the startup proceeds as with `initialize` above.

## Component Online and Offline
`Online` describes a component that is currently part of the observing system that is in use. 
When a component enters the Running state it is also "online".

A component is `offline` when it is operating and available for active observing but is not currently
in use.

If a component is to transition from the online state to the offline state, the `onGoOffLine`
handler is called. The component should make any changes in its operation for offline use.

If a component is to transition from the offline state to the online state, the `onGoOnline`
handler is called. The component should make any changes in its operation needed for online use.

@@@ note

Unless implemented by the developer, there is no fundamental difference in the inherent behavior of a component when in 
either state.  These two states provide a standard way for code to be implemented via these handlers for the transition
from one state to another, allowing the component to prepare itself to be online (ready for operations) or offline (stowed 
or dormant).  Any call to transition to a online/offline state when the component is already in that state is a no op.

@@@

### isOnline

A component has access to the `isOnline` boolean flag, which can be used to determine if the component is in the online or offline state.

### onGoOffline

A component can be notified to run in offline mode in case it is not in use. The component can change its behavior if needed as a part of this handler.

Assembly/Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/example/framework/components/assembly/AssemblyComponentHandlers.scala) { #onGoOffline-handler }

Assembly/Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/example/framework/components/assembly/JAssemblyComponentHandlers.java) { #onGoOffline-handler }

Hcd/Scala
:   @@snip [HcdComponentHandlers.scala](../../../../examples/src/main/scala/example/framework/components/hcd/HcdComponentHandlers.scala) { #onGoOffline-handler }

Hcd/Java
:   @@snip [JHcdComponentHandlers.java](../../../../examples/src/main/java/example/framework/components/hcd/JHcdComponentHandlers.java) { #onGoOffline-handler }

### onGoOnline

A component can be notified to run in online mode again in case it was put to run in offline mode. The component can change its behavior if needed as a part of this handler.

Assembly/Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/example/framework/components/assembly/AssemblyComponentHandlers.scala) { #onGoOnline-handler }

Assembly/Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/example/framework/components/assembly/JAssemblyComponentHandlers.java) { #onGoOnline-handler }

Hcd/Scala
:   @@snip [HcdComponentHandlers.scala](../../../../examples/src/main/scala/example/framework/components/hcd/HcdComponentHandlers.scala) { #onGoOnline-handler }

Hcd/Java
:   @@snip [JHcdComponentHandlers.java](../../../../examples/src/main/java/example/framework/components/hcd/JHcdComponentHandlers.java) { #onGoOnline-handler }


## Handling commands

The remaining handlers are associated with handling incoming commands. There is a handler
for submit commands called `onSubmit` and a handler for oneway called `onOneway`.

This section gives an introduction to the command handlers.  For more information on how to send and monitor commands,
see the @ref:[Communication using Commands](../commons/command.md) page.

### validateCommand

The `validateCommand` handler allows the component to inspect a command and its parameters to determine if the actions related to the command
can be executed or started. If it is okay, an `Accepted` response is returned.  If not, `Invalid` is returned. Validation may 
also take into consideration the state of the component. For instance, if an Assembly or HCD can only handle one command
at a time, `validateCommand` should return an return `Invalid` if a second command is received.

The handler is called whenever a command is sent as a `Submit` or `Oneway` message to the component. If the handler returns `Accepted`,
the corresponding `onSubmit` or `onOneway` handler is called.   This handler can also be called when the Command Service method
`validateCommand` is used, to preview the acceptance of a command before it is sent using `submit` or `oneway`.  In this case, 
the `onSubmit` or `onOneway` handler is not called.

Assembly/Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/example/framework/components/assembly/AssemblyComponentHandlers.scala) { #validateCommand-handler }

Assembly/Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/example/framework/components/assembly/JAssemblyComponentHandlers.java) { #validateCommand-handler }

Hcd/Scala
:   @@snip [HcdComponentHandlers.scala](../../../../examples/src/main/scala/example/framework/components/hcd/HcdComponentHandlers.scala) { #validateCommand-handler }

Hcd/Java
:   @@snip [JHcdComponentHandlers.java](../../../../examples/src/main/java/example/framework/components/hcd/JHcdComponentHandlers.java) { #validateCommand-handler }

### onSubmit

On receiving a command sent using the `submit` message, the `onSubmit` handler is invoked for a component only if the `validateCommand` handler returns `Accepted`. 
The `onSubmit` handler returns a `SubmitResponse` indicating if the command is completed immediately, or if it is long-running by returning a `Started` response. 
Completion of a long running command is then tracked using the `CommandResponseManager`, described in more detail in the
@ref:[Managing Command State](managing-command-state.md) page.  

The example shows one way to process `Setup` and `Observe` commands separately.

Assembly/Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/example/framework/components/assembly/AssemblyComponentHandlers.scala) { #onSubmit-handler }

Assembly/Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/example/framework/components/assembly/JAssemblyComponentHandlers.java) { #onSubmit-handler }

### onOneway

On receiving a command as `oneway`, the `onOneway` handler is invoked for a component only if the `validateCommand` handler returns `Accepted`.
The `onOneway` handler does not return a value and a command submitted with the `oneway` does not track completion of actions. 

Assembly/Scala
:   @@snip [AssemblyComponentHandlers.scala](../../../../examples/src/main/scala/example/framework/components/assembly/AssemblyComponentHandlers.scala) { #onOneway-handler }

Assembly/Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/example/framework/components/assembly/JAssemblyComponentHandlers.java) { #onOneway-handler }

## Handling Diagnostic Data

###  onDiagnosticMode

A Component can receive a `DiagnosticMode` command from other components at any time. The `onDiagnosticMode` handler
of the component is invoked on receiving the command.
Inside the handler, the components can start publishing some diagnostic data as desired.
The `DiagnosticMode` command can be received by a component only in the Running state and is ignored otherwise.
The diagnosticMode handler contains a `startTime` which is a @scaladoc[UTCTime](csw/time/core/models/UTCTime) 
and a String parameter called `hint` with the name of the technical data mode. The component should read the hint and 
publish events accordingly.

@@@note

A component developer should be careful to make any changes in the component's internal state in any callbacks. For example,
 while calling `timeServiceScheduler.schedule` or `eventPublisher.publish`, if you are trying to mutate state in 
 the callbacks passed to them, you might run into concurrency issues. Hence, in such scenarios, it is recommended to
 use a `WorkerActor` to manage the state.

@@@

The startTime is included so a diagnosticMode can be synchronized in time with diagnosticMode starting in other components.
The Time Service can be used to schedule execution of some tasks at the specified startTime.
Event Service publish Api can also be used in order to start publishing events at the specified startTime.
A component can only be in one technical data mode at a time. If the component is in one technical data, then on
receiving command to go in another technical mode, the component should stop/halt the previous  
diagnosticMode handler, and should enter the new technical data mode.
Even if the component does not define any diagnostic modes, it must be prepared to receive and process diagnosticMode 
handler without an error by completing with no changes.

The supported diagnostic mode hints of a component are published in the component's model files.
Unsupported hints should be rejected by a component.

The example shows one usage of `onDiagnosticMode` handler.

Scala
:   @@snip [SampleComponentHandlers.scala](../../../../csw-framework/src/test/scala/csw/common/components/framework/SampleComponentHandlers.scala) { #onDiagnostic-mode }

Java
:   @@snip [JSampleComponentHandlers.java](../../../../csw-framework/src/test/java/csw/framework/javadsl/components/JSampleComponentHandlers.java) { #onDiagnostic-mode }

### onOperationsMode

Components can receive an `OperationsMode` command which is used to halt all diagnostic modes. The `onOperationsMode` handler
of the component will be invoked on receiving this command.
Similar to `DiagnosticMode` command, the `OperationsMode` command is also handled only in the Running state and is ignored otherwise.
If in a technical data mode, the component should immediately halt its diagnostic mode and return to normal operations behavior.
Even if the component does not define any diagnostic modes, it must be prepared to receive and
process an `onOperationsMode` handler call. The component should return completion without error.

The example shows one usage of `onOperationsMode` handler.

Scala
:   @@snip [SampleComponentHandlers.scala](../../../../csw-framework/src/test/scala/csw/common/components/framework/SampleComponentHandlers.scala) { #onOperations-mode }

Java
:   @@snip [JSampleComponentHandlers.java](../../../../csw-framework/src/test/java/csw/framework/javadsl/components/JSampleComponentHandlers.java) { #onOperations-mode }