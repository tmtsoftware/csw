package csw.trombone.assembly.actors

sealed trait CommandExecutionState
object CommandExecutionState {
  case object NotFollowing extends CommandExecutionState
  case object Following    extends CommandExecutionState
  case object Executing    extends CommandExecutionState
}
