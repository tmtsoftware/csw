package csw.messages.ccs.commands

import akka.typed.ActorRef
import csw.messages.TMTSerializable
import csw.messages.ccs.CommandIssue
import csw.messages.ccs.commands.CommandResultType.{Intermediate, Negative, Positive}
import csw.messages.params.models.RunId

sealed abstract class CommandResponse extends TMTSerializable {
  def runId: RunId
  def resultType: CommandResultType
  def withRunId(id: RunId): CommandResponse
}

sealed abstract class CommandValidationResponse(val resultType: CommandResultType) extends CommandResponse
object CommandValidationResponse {
  case class Accepted(runId: RunId) extends CommandValidationResponse(Intermediate) {
    override def withRunId(id: RunId): CommandResponse = this.copy(runId = id)
  }
  case class Invalid(runId: RunId, issue: CommandIssue) extends CommandValidationResponse(Negative) {
    override def withRunId(id: RunId): CommandResponse = this.copy(runId = id)
  }
}

sealed abstract class CommandExecutionResponse(val resultType: CommandResultType) extends CommandResponse
object CommandExecutionResponse {
  case class InProgress(runId: RunId, message: String = "") extends CommandExecutionResponse(Intermediate) {
    override def withRunId(id: RunId): CommandResponse = this.copy(runId = id)
  }
  case class Initialized(runId: RunId) extends CommandExecutionResponse(Intermediate) {
    override def withRunId(id: RunId): CommandResponse = this.copy(runId = id)
  }

  case class CompletedWithResult(runId: RunId, result: Result) extends CommandExecutionResponse(Positive) {
    override def withRunId(id: RunId): CommandResponse = this.copy(runId = id)
  }
  case class Completed(runId: RunId) extends CommandExecutionResponse(Positive) {
    override def withRunId(id: RunId): CommandResponse = this.copy(runId = id)
  }
  case class BehaviorChanged[T](runId: RunId, ref: ActorRef[T]) extends CommandExecutionResponse(Positive) {
    override def withRunId(id: RunId): CommandResponse = this.copy(runId = id)
  }

  case class NoLongerValid(runId: RunId, issue: CommandIssue) extends CommandExecutionResponse(Negative) {
    override def withRunId(id: RunId): CommandResponse = this.copy(runId = id)
  }
  case class Error(runId: RunId, message: String) extends CommandExecutionResponse(Negative) {
    override def withRunId(id: RunId): CommandResponse = this.copy(runId = id)
  }
  case class Aborted(runId: RunId) extends CommandExecutionResponse(Negative) {
    override def withRunId(id: RunId): CommandResponse = this.copy(runId = id)
  }
  case class Cancelled(runId: RunId) extends CommandExecutionResponse(Negative) {
    override def withRunId(id: RunId): CommandResponse = this.copy(runId = id)
  }
  case class CommandNotAvailable(runId: RunId) extends CommandExecutionResponse(Negative) {
    override def withRunId(id: RunId): CommandResponse = this.copy(runId = id)
  }
}

sealed trait CommandResultType
object CommandResultType {
  case object Intermediate extends CommandResultType
  sealed trait Final       extends CommandResultType
  case object Positive     extends Final
  case object Negative     extends Final
}
