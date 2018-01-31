# Communication using Commands

**csw-command** library provides support for command based communication between components. 

## Artifacts

sbt
:   @@@vars
    ```scala
    libraryDependencies += "org.tmt" %% "csw-command" % "$version$"
    ```
    @@@

maven
:   @@@vars
    ```xml
    <dependency>
     <groupId>org.tmt</groupId>
     <artifactId>csw-command_$scala.binaryVersion$</artifactId>
     <version>$version$</version>
     <type>pom</type>
    </dependency>
    ```
    @@@

gradle
:   @@@vars
    ```gradle
    dependencies {
      compile group: "org.tmt", name: "csw-command_$scala.binaryVersion$", version: "$version$"
    }
    ```
    @@@
    
## Command based communication between components

A component can send @ref:[Commands](services/messages/commands.md) to other components. The commands can be sent as following 
two types of messages: 

* **Submit** - A command is sent as Submit when the result of completion is desired.
* **Oneway** - A command is sent as Oneway when the result of completion is not desired.

Following responses could be received as a `CommandResponse` after sending a command :
 
* **Accepted** : The command is validated and will be executed
* **Invalid** : The command is not valid and will not be executed 
* **CompletedWithResult** : The command is executed successfully and generated some result
* **Completed** : The command has been executed successfully
* **NoLongerValid** : The command can no longer be executed
* **Error** : The command has failed in execution
* **Cancelled** : The command was cancelled
* **CommandNotAvailable** : The queried command is not available
* **NotAllowed** : The command cannot be executed currently because of the current state of the destination component. Eg 
another command is in execution in the presence of which it cannot accept any other command to execute.

A command sent as `Submit` or `Oneway` is validated by the receiving component before actual execution. If the validation is successful, 
the actual execution can happen in two ways :

* **Immediate** - The component receiving the command can determine if the command can be executed immediately and thus provide the
final execution response directly without sending a response for validation.

Scala
:   @@snip [CommandServiceTest.scala](../../../csw-framework/src/multi-jvm/scala/csw/framework/command/CommandServiceTest.scala) { #immediate-response }

Java
:   @@snip [JCommandIntegrationTest.java](../../../csw-framework/src/test/java/csw/framework/command/JCommandIntegrationTest.java) { #immediate-response }

* **Long running** - The component receiving the command can determine if the command can not be executed immediately. In this case, the 
component provides a `Accepted` response as an acknowledgement and maintains the state of the command. The sender can query the state of 
a particular command or subscribe to get the final response when the execution is completed.

The sender component can send following commands to the receiver component with the command id (RunId) of the command being sent originally to 
get the completion status and/or result of the command

* **Query** - Query the current state of the command

Scala
:   @@snip [CommandServiceTest.scala](../../../csw-framework/src/multi-jvm/scala/csw/framework/command/LongRunningCommandTest.scala) { #query-response }

Java
:   @@snip [JCommandIntegrationTest.java](../../../csw-framework/src/test/java/csw/framework/command/JCommandIntegrationTest.java) { #query-response } 

* **Subscribe** - Subscribe for getting the final state of the command asynchronously

Scala
:   @@snip [LongRunningCommandTest.scala](../../../csw-framework/src/multi-jvm/scala/csw/framework/command/LongRunningCommandTest.scala) { #subscribe-for-result }

Java
:   @@snip [JCommandIntegrationTest.java](../../../csw-framework/src/test/java/csw/framework/command/JCommandIntegrationTest.java) { #subscribe-for-result }
 
## ComponentRef

When a component uses location service to discover another component, it is provided a `ComponentRef` model which not only 
wraps the actor reference of the component discovered, but also provides methods for communicating with the component using commands.
The API can be exercised as follows for different scenarios of command based communication:

### submit
### submitAll
### submitAllAndGetResponse
### oneway
### subscribe
### submitAndSubscribe
### submitAndMatch
### submitAllAndSubscribe
### submitAllAndGetFinalResponse

## Matching state for command completion

A `Matcher` is provided for matching state against a desired state. The matcher is created with a source of state identified
by its ActorRef and an instance of `StateMatcher` which defines the state and criteria for matching. Several instances of 
`StateMatcher` are available for common use. These are `DemandMatcherAll` for matching the entire `DemandState` against the current state,
`DemandMatcher` for matching state with or without units against the current state and `PresenceMatcher` which checks if a 
matching state is found with a provided prefix. 
  
Scala
:   @@snip [LongRunningCommandTest.scala](../../../csw-framework/src/multi-jvm/scala/csw/framework/command/CommandServiceTest.scala) { #matcher }

Java
:   @@snip [JCommandIntegrationTest.java](../../../csw-framework/src/test/java/csw/framework/command/JCommandIntegrationTest.java) { #matcher }

## Distributing commands

`CommandDistributor` is a utility for distributing commands to multiple components and get an aggregated response. 

### aggregated validation

A component can send one or more commands to one or more components using a `Map[ComponentRef, Set[ControlCommand]`, and get an aggregated response 
of validation as `Accepted` if all the commands were successfully validated. An `Error` response is returned otherwise

### aggregated completion response

A component can send one or more commands to one or more components using a `Map[ComponentRef, Set[ControlCommand]`. The utility handles subscribing for 
final completion result for all the commands post successful validation and get an aggregated response of completion as `Completed` if all the commands 
were successfully completed. An `Error` response is returned otherwise.



