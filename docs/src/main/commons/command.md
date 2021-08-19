# Communication using Commands

The **csw-command** library provides support for command based communication between components. 

This section describes how to communicate with any other component using commands. To check how to manage commands
received, please visit @ref:[Component Handlers](../framework/handling-lifecycle.md) and @ref:[Managing Command State](../framework/managing-command-state.md).

## Dependencies

sbt
:   @@@vars
    ```scala
    libraryDependencies += "com.github.tmtsoftware.csw" %% "csw-command" % "$version$"
    ```
    @@@

## Command-based Communication Between Components

A component can send @ref:[Commands](../params/commands.md) to other components. The commands can be sent as one of the following 
three types of messages: 

* **submit** - A command is sent using a `submit` when the result of completion is desired.
* **oneway** - A command is sent as `oneway` when the result of completion is not desired.
* **validate** - A command is sent, but it is only validated with no actions started.

Submit comes in two versions: `submit` and `submitAndWait`. The only difference is in how long-running commands
are treated. If a command starts long-running actions, `submit` returns a `Started` response immediately while `submitAndWait` will
wait for the long-running actions to complete before returning a final response. The `submitAndWait` is a composite of `submit` and `queryFinal`.

A `submitAndWait` is the most convenient way of sending a command from one component to another. When received, a `submitAndWait` command
is validated and if accepted, the actions associated with the command are executed or started. When the `submit` response that
is part of the `submitAndWait` is received, `submitAndWait` immediately does a `queryFinal`. For simple scenarios, this is the right command and
will be the most commonly used.

A `submit` can be used when the sender of a command needs to do additional work before long-running actions are completed. 
For instance, send another command to execute in parallel.  If commands are short, `submit` and `submitAndWait` behave
the same way. When the actions started by `submit` are long-running, the caller can
wait for the actions to complete if needed using the `queryFinal` call.

A `oneway` is primarily meant to be used between an Assembly and an HCD when no completion information is
desired.  It is also useful when tracking completion using a Matcher 
and current state values (see below) or events.

A `validate` message is used to ask a destination component to validate a command and determine if the command can
be executed. It does not execute the command and only returns the result of validation. In some scenarios,
it may be useful to test to see if a command can be executed prior to trying to execute the command.

### Component Locking

If a component is locked, any Command Service command will have a response of `Locked`. The corresponding handler will not
be called until the component is unlocked.  See @ref:[Creating a Component](create-component.md) for more information
on locking.  

Note that the code in the receiving component's handler does not need to return `Locked`. If the component has been Locked, the component's 
Supervisor returns the `Locked` response to the caller and the handler is not called.

### Command Validation
Command validations occur in two scenarios.  One is using the `validate` message as described above.  For example, `validate` 
could be used by an Assembly when it needs to send multiple commands to different HCDs and it wants
to first check that all the HCDs can execute their commands before sending a command to any of the HCDs.  
The second scenario is that it is always the first step in processing a command with `submit`, `submitAndWait`, or `oneway`.
  
If the receiving component is not locked, the
component's supervisor calls the `validateCommand` handler of the Top Level Actor. The developer code evaluates
and returns a `ValidateCommandResponse` as shown in the following table.  

| ValidateCommandResponse | Description |
| :---: | --- |
| Accepted | The command is valid and can be executed by the component. |
| Invalid | The command is not valid and cannot be executed. The response includes a reason in the form of a `CommandIssue` |
| Locked | The component is locked by some other command sender. The validation could not occur.


### The Submit Message
A `submit` message is sent with its @ref:[command](../params/commands.md) to a component destination. 
A `SubmitResponse` is returned to the caller when the `submit` message is used.
If the `validateCommand` handler returns `Accepted`, the framework calls the `onSubmit` handler of the Top Level
Actor. The `onSubmit` handler always returns a `SubmitResponse`.

####Immediate Completion Scenario (submit)
If the actions of the `submit` command take a very short time to complete, they may be completed by the 
`onSubmit` handler.  This is called *immediate completion*. The time for the actions to complete should be
less than 1 second. (Note: The framework will timeout if the destination does not return a response within 1 second.)
In this scenario with `onSubmit`, the values of `SubmitResponse` can be `Completed` or `Error`.
`Error` is returned when the actions could not be accomplished. This is different than `Invalid`, which indicates 
that the command could not be validated.

The immediate completion behavior is similar to a remote procedure call although the execution is entirely asynchronous. 
If the actions are successful, the `Completed` `SubmitResponse` is returned. If there is a result, the
`Completed` is returned with a parameter set of `Result` type that can be inspected by the sender.

####Long Running Actions Scenario (submit)
When actions take longer than 1 second, `onSubmit` should start the actions and return the `Started` `SubmitResponse`. The
`Started` response indicates to the framework that long-running actions have been started.

Once the long-running actions have started, the receiving component code must notify the framework when the actions are
completed. This is done be updating through the @ref:[Command Response Manager](../framework/managing-command-state.md).

In addition to the values returned for immediate completion, long running actions can return `Cancelled`.  If
the component supports a separate command to stop a long-running command, the stopped command should return `Cancelled` when
successfully cancelled.  The command that cancels the long running command should return `Completed`.

### The SubmitAndWait Message
A `submitAndWait` message is sent with its @ref:[command](../params/commands.md) to a component destination. 
A `SubmitResponse` is returned to the caller when the `submitAndWait` message is used.
If the `validateCommand` handler returns `Accepted`, the framework calls the `onSubmit` handler of the Top Level
Actor. The `onSubmit` handler always returns a `SubmitResponse`.

####Immediate Completion Scenario (submitAndWait)
If the actions of the `submitAndWait` command take a very short time to complete, `submitAndWait` behaves exactly
like `submit` as described above.

The immediate completion behavior is similar to a remote procedure call although the execution is entirely asynchronous. 
If the actions are successful, the `Completed` `SubmitResponse` is returned. If there is a result, the
`Completed` is returned with a parameter set of `Result` type that can be inspected by the sender.

####Long Running Actions Scenario (submitAndWait)
When actions take longer than 1 second, `onSubmit` should start the actions and return the `Started` `SubmitResponse`. The
`Started` response indicates to the framework that long-running actions have been started. In this case, `submitAndWait`
immediately executes a `queryFinal` command in order to wait for the final response.

As with the `submit` description, the receiving component code must notify the framework when the actions are
completed. This is done be updating the @ref:[Command Response Manager](../framework/managing-command-state.md).

#### SubmitResponse Summary 
The following table summarizes all the possible values for `SubmitResponse`.

| SubmitResponse | Description |
| :---: | --- |
| Invalid | The command is not valid and cannot be executed. The response includes a reason in the form of a `CommandIssue`.  `onSubmit` is not executed. |
| Completed(Result) | This response is returned when the actions associated with a command are completed. A result may or may not be returned. |
| Started | Returned by `onSubmit` when long-running actions have been started. |
| Error | Error is returned the the actions started by a command do not complete properly. A message is returned explaining the error. |
| Cancelled | The actions associated with a long-running command have been cancelled. |
| Locked | The component is locked by some other command sender. The validation could not occur. |

###The Oneway Message
The other option for sending a @ref:[command](../params/commands.md) to a component destination is the `oneway`
message. The central difference between `submit` and `oneway` is that `oneway` does not track or allow reporting
of completion information. It supports a *fire and forget* type of communication approach.

A `OnewayResponse` is returned to the caller when the `oneway` message is used. `Oneway` does validate
the command. If the component is not locked, the `validateCommand` handler is called.  
If the `validateCommand` handler returns `Accepted`, the framework calls the `onOneway` handler of the Top Level
Actor. However, the `onOneway` handler does not return a value. The sender of the `oneway` message receives
the result of the validation or the Locked indication.  

The following table summarizes all the possible values for `OnewayResponse`.

| OnewayResponse | Description |
| :---: | --- |
| Invalid | The command is not valid and cannot be executed. The response includes a reason in the form of a `CommandIssue`.  `onOneway` is not executed. |
| Accepted | Returned when validation succeeds and the command was passed to the `onOneway` handler. |
| Locked | The component is locked by some other command sender. The validation could not occur. |

`Oneway` is available as a higher performance option when an Assembly needs to send commands to an HCD but
doesn't really care about completion such as the case when demands are being sent to a motor. Validation
is still present to ensure the HCD supports the standalone operation requirement that allows the HCD to check that 
its input values are not invalid and it is not getting out of range values.

`Oneway` can be used with a *matcher*. The matcher can use CurrentState or even events from the Event Service
to determine completion. This can be more complicated than `submit`, but may be useful in some scenarios.

## CommandService

A helper/wrapper is provided called `CommandService` that provides a convenient way to use the Command Service 
with a component from the Location Service. 

A `CommandService` instance is created using an `AkkaLocation` of the receiving component, discovered from the Location Service.
This `CommandService` instance has methods for communicating with the component. A new `CommandService` is 
created for each component for which commands are to be sent.

@@@ note {title="Scala async library!" }

Note that the Scala examples are using async/await which simplifies handling the Futures, but is not necessary. The async/await library is not available in Java.

@@@

The API can be exercised as follows for different scenarios of command-based communication:

### submit
Sending a `submit` message with a command returns a `SubmitResponse` as a Future.
The Future returned by `submit` will be the final response in case of short-running command and may be a positive completion (`Completed`)
or a negative completion (`Invalid`, `Error`, `Cancelled`, `Locked`). The response may also be the `Started` response in the case of long-running actions. 
In the case of a long-running command the `Started` response can be used with either `query` or `queryFinal`  to obtain the final response.

This example shows a command using `submit` that returns `Started` followed by a `queryFinal` to get the final response.

Scala/submit w/queryFinal
:   @@snip [SampleAssemblyHandlers.scala](../../../../examples/src/main/scala/example/tutorial/basic/sampleassembly/SampleAssemblyHandlers.scala) { #submitAndQueryFinal }

Java/submit w/queryFinal
:   @@snip [JSampleAssemblyHandlers.java](../../../../examples/src/main/java/example/tutorial/basic/sampleassembly/JSampleAssemblyHandlers.java) { #submitAndQueryFinal }

If using `submit` and the validation fails in the destination component, the `Invalid` response is returned. Sometimes it
is possible to handle an `Invalid` response locally, but in most cases it must simply be returned to the caller. The following example
shows how to process individual responses from a `submit`:

Scala/submit w/invalid response
:   @@snip [CommandServiceTest.scala](../../../../integration/src/multi-jvm/scala/csw/framework/command/CommandServiceTest.scala) { #invalidCmd }

Java/submit w/invalid response
:   @@snip [JCommandIntegrationTest.java](../../../../csw-framework/src/test/java/csw/framework/command/JCommandIntegrationTest.java) { #invalidSubmitCmd }

The handling an immediate completion command looks the same from the command sender's perspective, but can be challenging on the side of the
component handling the command. Because `submit` returns a `Future[SubmitResponse]` and `onSetup` returns `SubmitResponse`, an immediate completion
command must be handled in the component that receives the request. For instance, an Assembly can return a `Completed` without returning `Started`, but
if an Assembly needs to contact an HCD to get a response, the Assembly must first return `Started` and then contact the HCD. The following snippet
shows a portion of an `onSetup` handler that returns some local state as an immediate completion. 

Scala/submit immediate command
:   @@snip [SampleAssemblyHandlers.scala](../../../../examples/src/main/scala/example/tutorial/basic/sampleassembly/SampleAssemblyHandlers.scala) { #immediate-command }

Java/submit immediate command
:   @@snip [JSampleAssemblyHandlers.java](../../../../examples/src/main/java/example/tutorial/basic/sampleassembly/JSampleAssemblyHandlers.java) { #immediate-command }

### submitAndWait
`submitAndWait` is a convenience method which sends a `submit` message and then, if the command is long-running, it waits for final completion.
Sending a `submit` message with a command returns a `SubmitResponse` as a Future.
The Future returned by `submitAndWait` will always be the final response, whether the actions are long-running or not.
The final result may be a positive completion (`Completed`) or a negative completion (`Invalid`, `Error`, `Cancelled`, `Locked`). 
The `Started` response is never seen by the programmer when using `submitAndWait` of `CommandService`. The handling of long-running and immediate completion 
commands look the same from the command sender's perspective when using `submitAndWait`.

This example shows an immediate completion command using `submitAndWait`.

Scala/submitAndWait w/immediate-response
:   @@snip [CommandServiceTest.scala](../../../../integration/src/multi-jvm/scala/csw/framework/command/CommandServiceTest.scala) { #immediate-response }

Java/submitAndWait w/immediate-response
:   @@snip [JCommandIntegrationTest.java](../../../../csw-framework/src/test/java/csw/framework/command/JCommandIntegrationTest.java) { #immediate-response }

Several examples have been shown of sending a command that is long-running. The following examples show an Assembly that issues a
command to an HCD with `submitAndWait` and returns `Started`. When `submitAndWait` returns with a final response, the parent is updated
with the final response through the `CommandResponseManager`.

Scala/submitAndWait w/immediate-response
:   @@snip [CommandAssemblyHandlers.scala](../../../../csw-framework/src/test/scala/csw/common/components/command/CommandAssemblyHandlers.scala) { #longRunning }

Java/submitAndWait w/immediate-response
:   @@snip [JCommandIntegrationTest.java](../../../../csw-framework/src/test/java/csw/framework/command/JCommandIntegrationTest.java) { #longRunning }



### oneway

`Oneway` does not provide completion information but does return the result of `validateCommand` handler
in the Top-Level-Actor (`Accepted`, `Invalid`, or `Locked`). 
When sending a command as a `oneway` message, a `OnewayResponse` is returned as a Future that can be used to check that it was validated if necessary.

Oneway is useful for communication between an Assembly when it needs to send commands to an HCD as quickly as possible. The command is
validated on the destination and the validation response is returned, but no other responses are provided.

Scala
:   @@snip [CommandServiceTest.scala](../../../../integration/src/multi-jvm/scala/csw/framework/command/CommandServiceTest.scala) { #oneway }

Java
:   @@snip [JCommandIntegrationTest.java](../../../../csw-framework/src/test/java/csw/framework/command/JCommandIntegrationTest.java) { #oneway }

### validate
Sometimes it may be useful to test whether or not a component can execute a command without committing to executing its
actions. The `validate` message can be used for this purpose. `Validate` returns a `ValidateResponse` of `Accepted`, `Invalid`, or `Locked`.

Scala
:   @@snip [CommandServiceTest.scala](../../../../integration/src/multi-jvm/scala/csw/framework/command/CommandServiceTest.scala) { #validate }

Java
:   @@snip [JCommandIntegrationTest.java](../../../../csw-framework/src/test/java/csw/framework/command/JCommandIntegrationTest.java) { #validate }

### query
If a command returns a `Started` response indicating it has long-running actions, and the sender needs to determine that the
actions have started properly, or wishes to poll the destination component for the final response, 
the `query` method of `CommandService` can be used as shown in the
following example without using the Future returned by `submitAndWait`, which provides the final completion notification.

Scala/submit w/query
:   @@snip [CommandServiceTest.scala](../../../../integration/src/multi-jvm/scala/csw/framework/command/CommandServiceTest.scala) { #queryLongRunning }

Java/submit w/query
:   @@snip [JCommandIntegrationTest.java](../../../../csw-framework/src/test/java/csw/framework/command/JCommandIntegrationTest.java) { #queryLongRunning }

### queryFinal

When using `submit` and the `submit` returns a `Started` response, the final completion reponse can be obtained using `queryFinal`. Like `query`, 
`queryFinal` uses the `Id` returned by `Started`. But in this case, rather than returning immediately like `query`, it waits and only
returns when the final `SubmitResponse` is sent. `queryFinal` is used exclusively with `submit` in the case where some other
activity must be done before the actions started by the `submit` complete. `submitAndWait` is just a `submit` + `queryFinal`.  It is also
possible to just start actions with `submitAndWait` and use the returned Future when you are ready. The following examples show
you can use the runId returned by `submitAndWait` with `queryFinal`.


Scala/submitAndWait long running/queryFinal
:   @@snip [CommandServiceTest.scala](../../../../integration/src/multi-jvm/scala/csw/framework/command/CommandServiceTest.scala) { #queryFinalWithSubmitAndWait }

Java/submitAndWait long running/queryFinal
:   @@snip [JCommandIntegrationTest.java](../../../../csw-framework/src/test/java/csw/framework/command/JCommandIntegrationTest.java) { #queryFinal }
 

### submitAllAndWait
`submitAllAndWait` can be used to send multiple commands sequentially to the same component.
This could be used to send initialization commands to an HCD, for instance. The
argument for `submitAllAndWait` is a list of commands. `submitAllAndWait` returns a list of `SubmitResponse`s -- one
for each command in the list.  While `submitAndWait` returns a `SubmitResponse`
as a Future, `submitAllAndWait` returns a list of `SubmitResponse`s as a future, which completes when
all the commands in the list have completed. 

Scala/query usage
:   @@snip [CommandServiceTest.scala](../../../../integration/src/multi-jvm/scala/csw/framework/command/CommandServiceTest.scala) { #submitAll }

Java/query usage
:   @@snip [JCommandIntegrationTest.java](../../../../csw-framework/src/test/java/csw/framework/command/JCommandIntegrationTest.java) { #submitAll }

In the first example, three commands are sent and the result is a list with three `SubmitResponse`s. 
The last one returned invalid and was not executed.

The commands in `submitAllAndWait` will execute sequentially, but each one must complete successfully for
the subsequent commands to be executed. If any one of the commands fails, `submitAllAndWait` stops and
the list is returned with the commands that are completed up to and including the command
that failed.

### subscribeCurrentState
This method provided by `CommandService` can be used to subscribe to 
the @ref:[CurrentState](../params/states.md) of a component by providing a callback that
is called with the arrival of every `CurrentState` item. `SubscribeCurrentState` returns a handle of the `CurrentStateSubscription` which should be used 
to unsubscribe the subscription.

@@@ note
Callbacks are not thread-safe on the JVM. If you are doing side effects/mutations inside the callback, you should ensure that it is done in a thread-safe way inside an actor. Here is an [example]($github.base_url$/examples/src/main/scala/example/event/ConcurrencyInCallbacksExample.scala) of how it can be done.
@@@

`CurrentState` can be an efficient way to keep the Aseembly up to date with the current internal state of the HCD.

The following example code shows an Assembly that subscribes to all `CurrentState` items of an HCD.
The example sends a `Setup` with an encoder parameter value to the HCD as a `oneway` message. In this example, sending this command
causes the HCD to publish `CurrentState` with the value that was sent to it.

Scala
:   @@snip [CommandServiceTest.scala](../../../../integration/src/multi-jvm/scala/csw/framework/command/CommandServiceTest.scala) { #subscribeCurrentState }

Java
:   @@snip [JCommandIntegrationTest.java](../../../../csw-framework/src/test/java/csw/framework/command/JCommandIntegrationTest.java) { #subscribeCurrentState }

The second part of the example shows the code in the HCD. When the HCD receives the `oneway` message, it extracts 
the encoder value and publishes a CurrentState item with the encoder parameter. 

Scala
:   @@snip [ComponentHandlerForCommand.scala](../../../../csw-framework/src/test/scala/csw/common/components/command/ComponentHandlerForCommand.scala) { #subscribeCurrentState }

Java
:   @@snip [JSampleComponentHandlers.java](../../../../csw-framework/src/test/java/csw/framework/javadsl/components/JSampleComponentHandlers.java) { #subscribeCurrentState }

There are two `subscribeCurrentState` methods in `CommandService`. The method shown in the above examples subscribes
the caller to *all* CurrentState published. Each `CurrentState` item has a `StateName`. A second signature for 
`subscribeCurrentState` can include a Set of `StateName` when the caller only needs some of the CurrentState
published by a component. 

## Matching State for Command Completion

The `matcher` is provided to allow a component sending a command to use `CurrentState` published by a 
component to determine when actions are complete. The expected case is an Assembly using the `CurrentState`
published by an HCD. When using a `submit` or `submitAndWait`, completion is determined in the destination. In some
scenarios, the Assembly may want to determine when actions are complete. This is what the
 `matcher` allows.
 
 To use this feature, the `oneway` message is used rather than `submit`. A `oneway` command is
 validated but the framework does not provide completion. Doing a query with the `runId` of a `oneway`
 will always return `CommandNotAvailable`, but `oneway` is perfect for use with a `matcher`.

The `matcher` is created with the ActorRef of the component that is the source of 
`CurrentState` and an instance of `StateMatcher`, which defines the state and criteria for matching. 

Several types of `StateMatcher` are provided as part of CSW for common use. 
These are `DemandMatcherAll` for matching the entire `DemandState` against the current state,
`DemandMatcher` for matching state with or without units against the current state, 
and `PresenceMatcher` which checks if a matching state is found with a provided prefix. 

The developer is not limited to these `StateMatcher`s. Any class the implements the `StateMatcher`
interface can be provided to a `Matcher`.
  
Scala
:   @@snip [LongRunningCommandTest.scala](../../../../integration/src/multi-jvm/scala/csw/framework/command/CommandServiceTest.scala) { #matcher }

Java
:   @@snip [JCommandIntegrationTest.java](../../../../csw-framework/src/test/java/csw/framework/command/JCommandIntegrationTest.java) { #matcher }

One important point is that the `matcher` is created and must be shutdown when you are finished with it using
the `stop` method of the matcher as shown in the example.

### onewayAndMatch
`CommandService` provides a short-cut called `onewayAndMatch` that combines a `oneway` and a `matcher`
 and implements much of the boilerplate of the previous example.
 
Scala
:   @@snip [CommandServiceTest.scala](../../../../integration/src/multi-jvm/scala/csw/framework/command/CommandServiceTest.scala) { #onewayAndMatch }

Java
:   @@snip [JCommandIntegrationTest.java](../../../../csw-framework/src/test/java/csw/framework/command/JCommandIntegrationTest.java) { #onewayAndMatch }


