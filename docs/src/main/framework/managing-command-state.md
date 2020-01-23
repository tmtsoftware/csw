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

### updateCommand
`updateCommand` is used to update the status of a `Started` command. The following example
simulates a worker that takes some time to complete. The `onSubmit` handler returns `Started` and later the actions
complete with `Completed`, which completes the command. 

Scala
:   @@snip [McsHcdComponentHandlers.scala](../../../../csw-framework/src/test/scala/csw/common/components/command/McsHcdComponentHandlers.scala) { #updateCommand }

Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../csw-framework/src/test/java/csw/framework/javadsl/components/JSampleComponentHandlers.java) { #updateCommand }


## Using the CRM with Subcommands

If while processing a received command, the component needs to create and send commands to other components (e.g. an Assembly
sending commands to one or more HCDs) it can use the CRM to help manage responses from the sub-commands. In this case,
the typical use case is that the sender such as an Assembly needs to send one or more sub-commands to HCDs and needs to
wait until all the sub-commands complete. It then makes a decision on how to update the original command received by the
Assembly based on the results of the sub-commands. The CRM provides a helper method to wait for one or more sub-commands. 
Then the `updateCommand` CRM method is used to update the original command.

### Using queryFinalAll
Use `addSubCommand` to associate sub-commands with a received command.

Scala
:   @@snip [McsAssemblyComponentHandlers.scala](../../../../csw-framework/src/test/scala/csw/common/components/command/McsAssemblyComponentHandlers.scala) { #queryF }

Java
:   @@snip [JCommandIntegrationTest.java](../../../../csw-framework/src/test/java/csw/framework/command/JCommandIntegrationTest.java) { #queryF }

@@@ note

It may be the case that the component wants to avoid automatic inference of a command based on the result of the
sub-commands. It should refrain from updating the status of the sub-commands in this case and update the status
of the parent command directly as required.

@@@