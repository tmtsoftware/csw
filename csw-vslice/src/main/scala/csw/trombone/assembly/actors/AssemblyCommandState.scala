package csw.trombone.assembly.actors

import csw.trombone.assembly.commands.AssemblyCommand

case class AssemblyCommandState(
    mayBeAssemblyCommand: Option[List[AssemblyCommand]],
    commandExecutionState: CommandExecutionState
)
