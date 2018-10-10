# Communication using Commands

**csw-command** library provides support for command based communication between components. 

This section describes how to communicate with any other component using commands. To check how to manage commands
received, please visit @ref:[Managing Command State](../framework/managing-command-state.md)

## Dependencies

sbt
:   @@@vars
    ```scala
    libraryDependencies += "com.github.tmtsoftware.csw" %% "csw-command" % "$version$"
    ```
    @@@

## Command-based Communication Between Components

A component can send @ref:[Commands](../messages/commands.md) to other components. The commands can be sent as following 
two types of messages: 

* **submit** - A command is sent as Submit when the result of completion is desired.
* **oneway** - A command is sent as Oneway when the result of completion is not desired.

A `oneway` is meant to be used between an Assembly and an HCD.  It is also used when tracking completion using a Matcher 
and current state values (see below).

@@@ warning { title='Feedback Needed on Future of Oneway' }

In the FDR prototype version of CSW, the `oneway` message was used between an Assembly and HCD to "fire and forget". 
A `oneway` command was sent to the destination and no validation or command completion is provided in order 
to be as efficient as possible. In this case completion information is provided through Command Service
pubsub and current state values.

In this release version of CSW, `oneway` _does_ provide validation to the caller, and this version of CSW Command Service 
also reinstates immediate completion, which was dropped in the CSW prototype release. The only feature left from the 
FDR version is that `oneway` tells the sender that there is no completion information provided.
 
Given these two changes, the use of `oneway` is not sufficiently different from submit. With this in mind we 
anticipate one of the two possibly futures for `oneway`:

1. `Oneway` should provide a clear difference with `submit` and go back to its original features with no
validation or command completion.
2. `Oneway` could be removed simplifying the command API to just `submit`.

The reason for `oneway` case 1 is to provide the best possible performance. A scenario is an actor that
is subscribed to a 20 Hz demand event and sends out motion commands to one or more HCDs based on a calculation 
using data in this event. At 20 Hz, it's probably not useful to get validation information. Nothing can be
done anyway. The receiver could log a message.  

However, maybe even in this case validation is useful and can be used by the sender to
understand problems in the receiver, so providing only `submit` is adequate.

We are looking for feedback.  Do you see a continued role for `oneway`?  Maybe there is another scenario
for `oneway`?  Please write us if with your recommendation for oneway -- and be warned that it may change
behavior or disappear in the next release based on feedback.

@@@

The following responses can be received as a `CommandResponse` after sending a command with `Submit` or `Oneway`:

* **Accepted** : The command is validated and will be executed, this is returned for a long-running action.
* **Completed** : The command has been executed successfully.
* **CompletedWithResult** : The command is executed successfully and generated some result as a parameter set.
* **Invalid** : The command is not valid and will not be executed. A reason is provided.
* **NoLongerValid** : The command can no longer be executed (will be deprecated)
* **Error** : The command has failed in execution. A reason is provided.
* **Cancelled** : The command was cancelled.
* **CommandNotAvailable** : A queried command is not available.
* **NotAllowed** : The command cannot be executed currently because of the current state of the destination component. Eg. 
another command is in execution in the presence of which it cannot accept any other command to execute or some other reason.

A command sent as `Submit` or `Oneway` is validated by the receiving component before actual execution. If the validation is successful, 
the actual execution can happen in two ways :

* **Immediate Completion** - The component receiving the command can determine if the command can be executed immediately and thus provide the
final execution response directly without sending a response for validation. This should be reserved for actions that do not take
long to complete.

Scala
:   @@snip [CommandServiceTest.scala](../../../../csw-framework/src/multi-jvm/scala/csw/framework/command/CommandServiceTest.scala) { #immediate-response }

Java
:   @@snip [JCommandIntegrationTest.java](../../../../csw-framework/src/test/java/csw/framework/command/JCommandIntegrationTest.java) { #immediate-response }

* **Long Running Actions** - The component receiving the command may determine that the command cannot be executed immediately. In this case, the 
component provides a `Accepted` response as an acknowledgement and maintains the state of the command. The sender can query the state of 
a particular command at a later time or use the subscribe method to get the final response when the execution is completed.

The sender component can use the following with the command id (RunId) of an executing command to 
get the current status, completion response and/or result of the command

* **Query** - Query the current state of an executing command

Scala
:   @@snip [CommandServiceTest.scala](../../../../csw-framework/src/multi-jvm/scala/csw/framework/command/LongRunningCommandTest.scala) { #query-response }

Java
:   @@snip [JCommandIntegrationTest.java](../../../../csw-framework/src/test/java/csw/framework/command/JCommandIntegrationTest.java) { #query-response } 

* **Subscribe** - It is also possible to subscribe to asynchronously get command response updates for an executing command. At least one response is always delivered.

Scala
:   @@snip [LongRunningCommandTest.scala](../../../../csw-framework/src/multi-jvm/scala/csw/framework/command/LongRunningCommandTest.scala) { #subscribe-for-result }

Java
:   @@snip [JCommandIntegrationTest.java](../../../../csw-framework/src/test/java/csw/framework/command/JCommandIntegrationTest.java) { #subscribe-for-result }
 
## CommandService

A helper/wrapper is provided called `CommandService` that provides a convenient way to use the Command Service with a component 
discovered using Location Service. A `CommandService` instance is created using the value from the Location Service.
This `CommandService` instance will has methods for communicating with the component. 

The API can be exercised as follows for different scenarios of command-based communication:

### submit
Submit a command and get a `CommandResponse` as a Future. The CommandResponse can be a response from validation (Accepted, Invalid) or a final Response
in case of immediate completion.

Scala/immediate-response
:   @@snip [CommandServiceTest.scala](../../../../csw-framework/src/multi-jvm/scala/csw/framework/command/CommandServiceTest.scala) { #immediate-response }

Java/immediate-response
:   @@snip [JCommandIntegrationTest.java](../../../../csw-framework/src/test/java/csw/framework/command/JCommandIntegrationTest.java) { #immediate-response }

Scala/validation-response
:   @@snip [CommandServiceTest.scala](../../../../csw-framework/src/multi-jvm/scala/csw/framework/command/CommandServiceTest.scala) { #submit }

Java/validation-response
:   @@snip [JCommandIntegrationTest.java](../../../../csw-framework/src/test/java/csw/framework/command/JCommandIntegrationTest.java) { #submit }
   
### oneway
Send a command as a Oneway and get a `CommandResponse` as a Future. The CommandResponse can be a response of validation (Accepted, Invalid) or a final Response.

Scala
:   @@snip [CommandServiceTest.scala](../../../../csw-framework/src/multi-jvm/scala/csw/framework/command/CommandServiceTest.scala) { #oneway }

Java
:   @@snip [JCommandIntegrationTest.java](../../../../csw-framework/src/test/java/csw/framework/command/JCommandIntegrationTest.java) { #oneway }

### subscribe
Subscribe for the result of a long-running command which was sent as Submit to get a `CommandResponse` as a Future.

Scala
:   @@snip [LongRunningCommandTest.scala](../../../../csw-framework/src/multi-jvm/scala/csw/framework/command/LongRunningCommandTest.scala) { #subscribe-for-result }

Java
:   @@snip [JCommandIntegrationTest.java](../../../../csw-framework/src/test/java/csw/framework/command/JCommandIntegrationTest.java) { #subscribe-for-result }

### query
Query for the result of a long-running command which was sent as Submit to get a `CommandResponse` as a Future.

Scala
:   @@snip [CommandServiceTest.scala](../../../../csw-framework/src/multi-jvm/scala/csw/framework/command/LongRunningCommandTest.scala) { #query-response }

Java
:   @@snip [JCommandIntegrationTest.java](../../../../csw-framework/src/test/java/csw/framework/command/JCommandIntegrationTest.java) { #query-response }

### submitAndSubscribe
Submit a command and Subscribe for the result if it was successfully validated as `Accepted` to get a final `CommandResponse` as a Future.

Scala
:   @@snip [CommandServiceTest.scala](../../../../csw-framework/src/multi-jvm/scala/csw/framework/command/LongRunningCommandTest.scala) { #submitAndSubscribe }

Java
:   @@snip [JCommandIntegrationTest.java](../../../../csw-framework/src/test/java/csw/framework/command/JCommandIntegrationTest.java) { #submitAndSubscribe }

### onewayAndMatch
Send a command and match the published state from the component using a `StateMatcher`. If the match is successful a `Completed` response is provided as a future. 
In case of a failure or unmatched state, `Error` CommandResponse is provided as a Future.

Scala
:   @@snip [CommandServiceTest.scala](../../../../csw-framework/src/multi-jvm/scala/csw/framework/command/CommandServiceTest.scala) { #onewayAndMatch }

Java
:   @@snip [JCommandIntegrationTest.java](../../../../csw-framework/src/test/java/csw/framework/command/JCommandIntegrationTest.java) { #onewayAndMatch }

### submitAllAndGetResponse
Submit multiple commands and get one CommandResponse as a Future of `CommandResponse` for all commands. If all the commands were successful, a CommandResponse 
as `Completed` will be returned. If any one of the command fails, an `Error` will be returned.

Scala
:   @@snip [CommandServiceTest.scala](../../../../csw-framework/src/multi-jvm/scala/csw/framework/command/LongRunningCommandTest.scala) { #submitAllAndGetResponse }

Java
:   @@snip [JCommandIntegrationTest.java](../../../../csw-framework/src/test/java/csw/framework/command/JCommandIntegrationTest.java) { #submitAllAndGetResponse }

### submitAllAndGetFinalResponse
Submit multiple commands and get final CommandResponse for all as one CommandResponse. If all the commands were successful, a CommandResponse as `Completed` will be 
returned. If any one of the command fails, an `Error` will be returned. For long running commands, it will subscribe for the result of those which were successfully 
validated as `Accepted` and get the final CommandResponse.

Scala
:   @@snip [CommandServiceTest.scala](../../../../csw-framework/src/multi-jvm/scala/csw/framework/command/LongRunningCommandTest.scala) { #submitAllAndGetFinalResponse }

Java
:   @@snip [JCommandIntegrationTest.java](../../../../csw-framework/src/test/java/csw/framework/command/JCommandIntegrationTest.java) { #submitAllAndGetFinalResponse }

### subscribeCurrentState
This method can be used to subscribe to the @ref:[CurrentState](../messages/states.md) of the component by providing a 
callback. Subscribing results into a handle of `CurrentStateSubscription` which can be used to unsubscribe the subscription.

Scala
:   @@snip [CommandServiceTest.scala](../../../../csw-framework/src/multi-jvm/scala/csw/framework/command/LongRunningCommandTest.scala) { #subscribeCurrentState }

Java
:   @@snip [JCommandIntegrationTest.java](../../../../csw-framework/src/test/java/csw/framework/command/JCommandIntegrationTest.java) { #subscribeCurrentState }

## Matching state for command completion

A `Matcher` is provided for matching state against a desired state. The matcher is created with a source of state identified
by its ActorRef and an instance of `StateMatcher` which defines the state and criteria for matching. Several instances of 
`StateMatcher` are available for common use. These are `DemandMatcherAll` for matching the entire `DemandState` against the current state,
`DemandMatcher` for matching state with or without units against the current state and `PresenceMatcher` which checks if a 
matching state is found with a provided prefix. 
  
Scala
:   @@snip [LongRunningCommandTest.scala](../../../../csw-framework/src/multi-jvm/scala/csw/framework/command/CommandServiceTest.scala) { #matcher }

Java
:   @@snip [JCommandIntegrationTest.java](../../../../csw-framework/src/test/java/csw/framework/command/JCommandIntegrationTest.java) { #matcher }

## Distributing commands

`CommandDistributor` is a utility for distributing commands to multiple components and get an aggregated response. 

### aggregated validation response

A component can send one or more commands to one or more components using a `Map[ComponentRef, Set[ControlCommand]`, and get an aggregated response 
of validation as `Accepted` if all the commands were successfully validated. An `Error` response is returned otherwise

Scala
:   @@snip [LongRunningCommandTest.scala](../../../../csw-framework/src/multi-jvm/scala/csw/framework/command/LongRunningCommandTest.scala) { #aggregated-validation }

Java
:   @@snip [JCommandIntegrationTest.java](../../../../csw-framework/src/test/java/csw/framework/command/JCommandIntegrationTest.java) { #aggregated-validation }

### aggregated completion response

A component can send one or more commands to one or more components using a `Map[ComponentRef, Set[ControlCommand]`. The utility handles subscribing for 
final completion result for all the commands post successful validation and get an aggregated response of completion as `Completed` if all the commands 
were successfully completed. An `Error` response is returned otherwise.

Scala
:   @@snip [LongRunningCommandTest.scala](../../../../csw-framework/src/multi-jvm/scala/csw/framework/command/LongRunningCommandTest.scala) { #aggregated-completion }

Java
:   @@snip [JCommandIntegrationTest.java](../../../../csw-framework/src/test/java/csw/framework/command/JCommandIntegrationTest.java) { #aggregated-completion }


