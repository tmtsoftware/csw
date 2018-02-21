## Managing Command State

A component has access to `commandResponseManager` which is used to manage the state of commands during its execution.
On receiving a command as a part of `onSubmit`, the component framework adds the command to an internal CommandResponseManager.
The component should then update the status of the command using the following API provided in `commandResponseManager`:

### addOrUpdateCommand
Add a new command or update the status of an existing command

Scala
:   @@snip [McsAssemblyComponentHandlers.scala](../../../../csw-framework/src/test/scala/csw/common/components/command/McsAssemblyComponentHandlers.scala) { #addOrUpdateCommand }

### addSubCommand
A received command can be often split into two or more sub-commands. The status of original command can then be derived
from the status of the sub-commands. In order to achieve this, a component has to first add the sub-commands against 
in relation to the parent command using this method.

Scala
:   @@snip [McsAssemblyComponentHandlers.scala](../../../../csw-framework/src/test/scala/csw/common/components/command/McsAssemblyComponentHandlers.scala) { #addSubCommand }
 

### updateSubCommand
Update the status of the sub-command which would trigger the automatic derivation of the status of the original/parent command when
status of all the sub-commands have been updated. A status indicating failure such as `Invalid` or `Error` in any one 
of the sub-commands would result in the error status of the parent command. Status of any other sub-commands wil not be 
considered in this case.

Scala
:   @@snip [McsAssemblyComponentHandlers.scala](../../../../csw-framework/src/test/scala/csw/common/components/command/McsAssemblyComponentHandlers.scala) { #updateSubCommand }

@@@ note

It may be the case that the component wants to avoid automatic inference of a command based on the result of the
sub-commands. It should refrain from updating the status of the sub-commands in this case and update the status
of the parent command directly as required.

@@@

### query
Query for the result of a command that is already present in the component's CommandResponseManager.

Scala
:   @@snip [McsAssemblyComponentHandlers.scala](../../../../csw-framework/src/test/scala/csw/common/components/command/McsAssemblyComponentHandlers.scala) { #subscribe-to-command-response-manager }

### subscribe
Subscribe for the result of a command that is already present in the component's CommandResponseManager and perform action
on the change in status.

Scala
:   @@snip [McsAssemblyComponentHandlers.scala](../../../../csw-framework/src/test/scala/csw/common/components/command/McsAssemblyComponentHandlers.scala) { #subscribe-to-command-response-manager }
