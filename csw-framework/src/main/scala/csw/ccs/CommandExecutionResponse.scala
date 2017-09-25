package csw.ccs

import akka.typed.ActorRef
import csw.param.commands.Result

/**
 * Trait for a response message from an assembly to a submit or observe request
 */
sealed trait CommandResponse
sealed trait CommandExecutionResponse  extends CommandResponse
sealed trait CommandValidationResponse extends CommandResponse
object CommandValidationResponse {

  /**
   * Converts a validation result to a CommandStatus result
   *
   * @param validation the result of a validation either Validation.Valid or Validation.Invalid
   * @return cooresponding CommandStatus as CommandStatus.Valid or CommandStatus.Invalid with the identical issue
   */
  def validationAsCommandStatus(validation: Validation): CommandValidationResponse = {
    validation match {
      case Validations.Valid        => Accepted
      case inv: Validations.Invalid => Invalid(inv.issue)
    }
  }
}

/**
 * The configuration was not valid before starting
 * @param issue an issue that caused the input configuration to be invalid
 */
final case class Invalid(issue: ValidationIssue) extends CommandValidationResponse

object Invalid {
  // This is present to support returning a Validation as a CommandStatus
  def apply(in: Validations.Invalid): Invalid = new Invalid(in.issue)

  /**
   * Java API: This is present to support returning a Validation as a CommandStatus
   */
  def createInvalid(in: Validations.Invalid): Invalid = Invalid(in)
}

/**
 * The configuration was valid and started
 */
case object Accepted extends CommandValidationResponse

/**
 * Command Completed with a result
 * @param result - Result ParamSet to types in Configuration and use it here
 */
final case class CompletedWithResult(result: Result) extends CommandExecutionResponse

/**
 * The command was valid when received, but is no longer valid because of itervening activities
 */
final case class NoLongerValid(issue: ValidationIssue) extends CommandExecutionResponse

/**
 * The command has completed successfully
 */
case object Completed extends CommandExecutionResponse

/**
 * The command is currently executing or has not yet started
 * When used for a specific command, it indicates the command has not yet executed or is currently executing and is providing an update
 */
final case class InProgress(message: String = "") extends CommandExecutionResponse

/**
 * The command was started, but ended with error with the given message
 */
final case class Error(message: String) extends CommandExecutionResponse

/**
 * The command was aborted
 * Aborted means that the command/actions were stopped immediately.
 */
case object Aborted extends CommandExecutionResponse

/**
 * The command was cancelled
 * Cancelled means the command/actions were stopped at the next convenient place. This is usually appropriate for
 */
case object Cancelled extends CommandExecutionResponse

case class BehaviorChanged[T](ref: ActorRef[T]) extends CommandExecutionResponse
