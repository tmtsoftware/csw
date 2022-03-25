# Managing Command State

A component is provided with a `commandResponseManager` which is used to update the state of commands that start long-running
actions. A long-running command is one that starts actions that take longer than 1 second.

The CommandResponseManager (CRM) is used to provide the final `SubmitResponse` in the following two scenarios. 
These scenarios require the developer to update the CRM.

1. The `onSubmit` handler returns `Started` indicating a long-running command that requires notification of 
completion at a later time.
2. To process an `onSubmit` that starts long-running actions, the component needs to send one or more commands to other
components that may also take time to complete.

On receiving a command as a part of `onSubmit`, and if the `onSubmit` handler returns `Started`, 
the framework adds the command to an internal CommandResponseManager that keeps track of the command and the sender
of the command. The sender is then sent the final `SubmitResponse` when CRM `updateCommand` is called. 

## Updating a Long-running Command

In the first scenario, the developer has a long-running command. In this case, once
the actions are completed, `updateCommand` is used to notify the CRM that the actions are complete. This will cause
the original sender to be notified of completion using the `SubmitResponse` passed to `updateCommand`.

### Using updateCommand

`updateCommand` is used to update the status of a `Started` command. The following example from the SampleAssembly
shows the Assembly sends a command to SampleHcd. It then does a `queryFinal` and when it returns, it
updates the parent runId with the response received from the HCD. 
The `onSubmit` handler (not shown) already has returned `Started` to the sender of the original command,
and the asynchronous completion is used to update the parent command. 

Scala
:   @@snip [SampleHcdHandlers.scala](../../../../examples/src/main/scala/example/tutorial/basic/sampleassembly/SampleAssemblyHandlers.scala) { #updateCommand }

Java
:   @@snip [JSampleHcdHandlers.java](../../../../examples/src/main/java/example/tutorial/basic/sampleassembly/JSampleAssemblyHandlers.java) { #updateCommand }


## Using the CRM with Subcommands

If while processing a received command, the component needs to create and send commands to other components (e.g. an Assembly
sending commands to one or more HCDs) it can use the CRM to help manage responses from the sub-commands. In this case,
the typical use case is that the sender such as an Assembly needs to send one or more sub-commands to HCDs and needs to
wait until all the sub-commands complete. It then makes a decision on how to update the original command received by the
Assembly based on the results of the sub-commands. The CRM provides a helper method to wait for one or more sub-commands. 
Then the previous `updateCommand` CRM method is used to update the original command.

### Using queryFinalAll
The CRM provides a method called `queryFinalAll`. This method takes a list of responses from `submit` or `submitAndWait`
and allows a block of code to be completed when *all* the commands in the list have completed, either successfully or unsuccessfully.
A response is returned from `queryFinalAll` of type `OverallSuccess`, which can be `OverallSuccess` or `OverallFailure`. Each of
these returns the individual responses from the original commands to allow a decision on how to proceed.

In this example of a `complexCommand`, the Assembly sends two *sub-commands* to HCDs. It then uses `queryFinalAll` to wait for the
sub-commands to finish. In the `OverallSuccess` case `commandResponseManager.updateCommand` is used to return `Completed` to the parent.
If one or more of the sub-commands fails, the negative response of the first failed command is returned to the parent.

Scala
:   @@snip [SampleAssemblyHandlers.scala](../../../../examples/src/main/scala/example/tutorial/basic/sampleassembly/SampleAssemblyHandlers.scala) { #queryF }

Java
:   @@snip [JCommandIntegrationTest.java](../../../../examples/src/main/java/example/tutorial/basic/sampleassembly/JSampleAssemblyHandlers.java) { #queryF }
