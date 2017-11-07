package csw.messages.ccs.commands

import akka.typed.ActorRef
import csw.messages.TMTSerializable
import csw.messages.ccs.CommandIssue
import csw.messages.params.models.RunId

/**
 * Trait for a response message from an assembly to a submit or observe request
 */
sealed trait CommandResponse extends TMTSerializable {
  val runId: RunId
}

sealed trait CommandStateType
sealed trait CommandFinalResponse        extends CommandStateType
sealed trait CommandIntermediateResponse extends CommandStateType

sealed trait CommandResultType
sealed trait CommandPositiveResponse extends CommandResultType with CommandFinalResponse
sealed trait CommandNegativeResponse extends CommandResultType with CommandFinalResponse

sealed trait CommandValidationResponse extends CommandResponse
object CommandValidationResponse {
  final case class Accepted(runId: RunId) extends CommandValidationResponse with CommandIntermediateResponse
  final case class Invalid(runId: RunId, issue: CommandIssue)
      extends CommandValidationResponse
      with CommandNegativeResponse
}

sealed trait CommandExecutionResponse extends CommandResponse

sealed trait CommandFinalExecutionResponse extends CommandExecutionResponse
object CommandFinalExecutionResponse {

  /**
   * Command Completed with a result
   * @param result - Result ParamSet to types in Configuration and use it here
   */
  final case class CompletedWithResult(runId: RunId, result: Result)
      extends CommandFinalExecutionResponse
      with CommandPositiveResponse

  /**
   * The command was valid when received, but is no longer valid because of intervening activities
   */
  final case class NoLongerValid(runId: RunId, issue: CommandIssue)
      extends CommandFinalExecutionResponse
      with CommandNegativeResponse

  /**
   * The command has completed successfully
   */
  final case class Completed(runId: RunId) extends CommandFinalExecutionResponse with CommandPositiveResponse

  /**
   * The command was started, but ended with error with the given message
   */
  final case class Error(runId: RunId, message: String)
      extends CommandFinalExecutionResponse
      with CommandNegativeResponse

  /**
   * The command was aborted
   * Aborted means that the command/actions were stopped immediately.
   */
  final case class Aborted(runId: RunId) extends CommandFinalExecutionResponse with CommandNegativeResponse

  /**
   * The command was cancelled
   * Cancelled means the command/actions were stopped at the next convenient place. This is usually appropriate for
   */
  final case class Cancelled(runId: RunId) extends CommandFinalExecutionResponse with CommandNegativeResponse

  final case class BehaviorChanged[T](runId: RunId, ref: ActorRef[T])
      extends CommandFinalExecutionResponse
      with CommandFinalResponse

  final case class CommandNotAvailable(runId: RunId) extends CommandFinalExecutionResponse with CommandNegativeResponse
}

sealed trait CommandIntermediateExecutionResponse extends CommandExecutionResponse with CommandIntermediateResponse
object CommandIntermediateExecutionResponse {

  /**
   * The command is currently executing or has not yet started
   * When used for a specific command, it indicates the command has not yet executed or is currently executing and is providing an update
   */
  final case class InProgress(runId: RunId, message: String = "")
      extends CommandFinalExecutionResponse
      with CommandIntermediateResponse

  final case class Initialized(runId: RunId) extends CommandFinalExecutionResponse with CommandIntermediateResponse
}
