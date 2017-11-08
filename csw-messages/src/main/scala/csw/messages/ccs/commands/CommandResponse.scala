package csw.messages.ccs.commands

import akka.typed.ActorRef
import csw.messages.TMTSerializable
import csw.messages.ccs.CommandIssue
import csw.messages.params.models.RunId

sealed abstract class CommandResponse extends TMTSerializable {
  def runId: RunId
  def resultType: CommandResultType
}

sealed abstract class CommandValidationResponse(val resultType: CommandResultType) extends CommandResponse
object CommandValidationResponse {
  case class Accepted(runId: RunId) extends CommandValidationResponse(CommandResultType.Intermediate)
  case class Invalid(runId: RunId, issue: CommandIssue) extends CommandValidationResponse(CommandResultType.Negative)
}

sealed abstract class CommandExecutionResponse(val resultType: CommandResultType) extends CommandResponse
object CommandExecutionResponse {
  final case class InProgress(runId: RunId, message: String = "") extends CommandExecutionResponse(CommandResultType.Intermediate)
  final case class Initialized(runId: RunId)                      extends CommandExecutionResponse(CommandResultType.Intermediate)


  case class CompletedWithResult(runId: RunId, result: Result) extends CommandExecutionResponse(CommandResultType.Positive)
  case class Completed(runId: RunId) extends CommandExecutionResponse(CommandResultType.Positive)
  case class BehaviorChanged[T](runId: RunId, ref: ActorRef[T]) extends CommandExecutionResponse(CommandResultType.Positive)

  case class NoLongerValid(runId: RunId, issue: CommandIssue) extends CommandExecutionResponse(CommandResultType.Negative)
  case class Error(runId: RunId, message: String) extends CommandExecutionResponse(CommandResultType.Negative)
  case class Aborted(runId: RunId) extends CommandExecutionResponse(CommandResultType.Negative)
  case class Cancelled(runId: RunId) extends CommandExecutionResponse(CommandResultType.Negative)
  case class CommandNotAvailable(runId: RunId) extends CommandExecutionResponse(CommandResultType.Negative)
}

sealed trait CommandResultType
object CommandResultType {
  case object Intermediate extends CommandResultType
  sealed trait Final extends CommandResultType
  case object Positive extends Final
  case object Negative extends Final
}
