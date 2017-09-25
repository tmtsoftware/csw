package csw.ccs

import akka.typed.ActorRef
import csw.param.commands.Result

/**
 * Trait for a response message from an assembly to a submit or observe request
 */
sealed trait CommandResponse
object CommandResponse {

  /**
   * Converts a validation result to a CommandStatus result
   *
   * @param validation the result of a validation either Validation.Valid or Validation.Invalid
   * @return cooresponding CommandStatus as CommandStatus.Valid or CommandStatus.Invalid with the identical issue
   */
  def validationAsCommandStatus(validation: Validation): CommandResponse = {
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
final case class Invalid(issue: ValidationIssue) extends CommandResponse

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
case object Accepted extends CommandResponse

/**
 * Command Completed with a result
 * @param result - Result ParamSet to types in Configuration and use it here
 */
final case class CompletedWithResult(result: Result) extends CommandResponse

/**
 * The command was valid when received, but is no longer valid because of itervening activities
 */
final case class NoLongerValid(issue: ValidationIssue) extends CommandResponse

/**
 * The command has completed successfully
 */
case object Completed extends CommandResponse

/**
 * The command is currently executing or has not yet started
 * When used for a specific command, it indicates the command has not yet executed or is currently executing and is providing an update
 */
final case class InProgress(message: String = "") extends CommandResponse

/**
 * The command was started, but ended with error with the given message
 */
final case class Error(message: String) extends CommandResponse

/**
 * The command was aborted
 * Aborted means that the command/actions were stopped immediately.
 */
case object Aborted extends CommandResponse

/**
 * The command was cancelled
 * Cancelled means the command/actions were stopped at the next convenient place. This is usually appropriate for
 */
case object Cancelled extends CommandResponse

case class BehaviorChanged[T](ref: ActorRef[T]) extends CommandResponse
