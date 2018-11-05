# Managing Command State

A component has access to `commandResponseManager` which is used to manage the state of commands during its execution.
On receiving a command as a part of `onSubmit` and if the command is accepted by the component, 
the framework adds the command to an internal CommandResponseManager (CRM).
The framework also uses the `SubmitResponse` returned by the `onSubmit` handler to update the CommandResponseManager. 
In many cases, this is adequate and no other information is required to handle completion information.

The CommandResponseManager can provide additional support in the following scenarios. These scenarios require
the developer to update the CRM.

1. The command received in `onSubmit` returns `Started` indicating a long-running command that requires notification of 
completion at a later time.
2. To process an `onSubmit` that starts long-running actions, the component needs to send one or more commands to other
components that may also take time to complete. 

## Updating a Long-running Command

In the first scenario, the developer has a long-running command and does not start any sub-commands or
does not with to use the CRM to help manage subcommands. In this case, once
the actions are completed, `addOrUpdateCommand` is used to notify the CRM that the actions are complete. This will cause
the original sender to be notified of completion using the `SubmitResponse` passed to `addOrUpdateCommand`.

### addOrUpdateCommand
`AddOrUpdateCommand` is used to add a new command or update the status of an existing command. The following example
simulates a worker that takes some time to complete. The `onSubmit` handler returns `Started` and later the actions
complete with `Completed`, which completes the command. 

Scala
:   @@snip [McsAssemblyComponentHandlers.scala](../../../../csw-framework/src/test/scala/csw/common/components/command/McsHcdComponentHandlers.scala) { #addOrUpdateCommand }

Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../csw-framework/src/test/java/csw/framework/javadsl/components/JSampleComponentHandlers.java) { #addOrUpdateCommand }

## Using the CRM with Subcommands

If while processing a received command, the component needs to create and send commands to other components (e.g, an Assembly
sending commands to one or more HCDs) it can use the CRM to help manage responses from the sub-commands.

A received command that requires one or more sub-commands must first associate the sub-commands with the received
command using the `addSubCommand` CRM method. When the sub-commands complete, `updateSubCommand` is used to update
the status of sub-commands. 

The status of original command can then be derived from the status of the sub-commands and when all the sub-commands
have completed either successfully or not, the original command will complete and a response returned to the original
command sender.

## addSubCommand
Use `addSubCommand` to associate sub-commands with a received command.

Scala
:   @@snip [McsAssemblyComponentHandlers.scala](../../../../csw-framework/src/test/scala/csw/common/components/command/McsAssemblyComponentHandlers.scala) { #addSubCommand }

Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/csw/framework/components/assembly/JAssemblyComponentHandlers.java) { #addSubCommand }

## updateSubCommand
Use `updateSubCommand` to update the CRM with the `SubmitResponse` of the sub-commands. 
This can trigger the delivery of the status of the original/parent command when
status of all the sub-commands have been updated. A `SubmitResponse` indicating failure such as `Cancelled` or `Error` in any one 
of the sub-commands results in the error status of the parent command. Status of any other sub-commands wil not be 
considered in this case.

Scala
:   @@snip [McsAssemblyComponentHandlers.scala](../../../../csw-framework/src/test/scala/csw/common/components/command/McsAssemblyComponentHandlers.scala) { #updateSubCommand }

Java
:   @@snip [JAssemblyComponentHandlers.java](../../../../examples/src/main/java/csw/framework/components/assembly/JAssemblyComponentHandlers.java) { #updateSubCommand }

@@@ note

It may be the case that the component wants to avoid automatic inference of a command based on the result of the
sub-commands. It should refrain from updating the status of the sub-commands in this case and update the status
of the parent command directly as required.

@@@