package csw.messages.ccs.commands

import akka.typed.ActorRef
import csw.messages.TMTSerializable
import csw.messages.ccs.CommandIssue
import csw.messages.ccs.commands.CommandResultType.{Intermediate, Negative, Positive}
import csw.messages.params.models.RunId

sealed abstract class CommandResponse extends TMTSerializable {
  def runId: RunId
  def resultType: CommandResultType
}

sealed abstract class CommandValidationResponse(val resultType: CommandResultType) extends CommandResponse
object CommandValidationResponse {
  case class Accepted(runId: RunId)                     extends CommandValidationResponse(Intermediate)
  case class Invalid(runId: RunId, issue: CommandIssue) extends CommandValidationResponse(Negative)
}

sealed abstract class CommandExecutionResponse(val resultType: CommandResultType) extends CommandResponse
object CommandExecutionResponse {
  case class InProgress(runId: RunId, message: String = "") extends CommandExecutionResponse(Intermediate)
  case class Initialized(runId: RunId)                      extends CommandExecutionResponse(Intermediate)

  case class CompletedWithResult(runId: RunId, result: Result)  extends CommandExecutionResponse(Positive)
  case class Completed(runId: RunId)                            extends CommandExecutionResponse(Positive)
  case class BehaviorChanged[T](runId: RunId, ref: ActorRef[T]) extends CommandExecutionResponse(Positive)

  case class NoLongerValid(runId: RunId, issue: CommandIssue) extends CommandExecutionResponse(Negative)
  case class Error(runId: RunId, message: String)             extends CommandExecutionResponse(Negative)
  case class Aborted(runId: RunId)                            extends CommandExecutionResponse(Negative)
  case class Cancelled(runId: RunId)                          extends CommandExecutionResponse(Negative)
  case class CommandNotAvailable(runId: RunId)                extends CommandExecutionResponse(Negative)
}

sealed trait CommandResultType
object CommandResultType {
  case object Intermediate extends CommandResultType
  sealed trait Final       extends CommandResultType
  case object Positive     extends Final
  case object Negative     extends Final
}
