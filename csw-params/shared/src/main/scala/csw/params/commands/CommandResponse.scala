package csw.params.commands

import csw.params.TMTSerializable
import csw.params.commands.CommandResultType.{Intermediate, Negative, Positive}
import csw.params.core.models.Id
import enumeratum._

import scala.collection.immutable

/**
 * Parent type of a response of command Execution
 *
 *   @param resultType the nature of command response as [[csw.messages.commands.CommandResultType]]
 */
sealed abstract class CommandResponseBase() extends TMTSerializable {

  def resultType: CommandResultType
  /**
   * A helper method to get the runId for this command response
   *
   * @return the runId of command for which this response is created
   */
  def runId: Id
}

sealed abstract class ValidationResponse(val resultType: CommandResultType) extends CommandResponseBase
object ValidationResponse {

  /**
   * Represents an intermediate response stating acceptance of a command received
   *
   * @param runId the runId of command for which this response is created
   */
  case class Accepted(runId: Id) extends CommandResponse(Intermediate)

  /**
   * Represents a negative response invalidating a command received
   *
   * @param runId of command for which this response is created
   * @param issue describing the cause of invalidation
   */
  case class Invalid(runId: Id, issue: CommandIssue) extends CommandResponse(Negative)

  /**
   * Represents a positive response stating completion of command
   *
   * @param runId of command for which this response is created
   * @param result describing the result of completion
   */
  case class CompletedWithResult(runId: Id, result: Result) extends CommandResponse(Positive)

  /**
   * Represents a positive response stating completion of command
   *
   * @param runId of command for which this response is created
   */
  case class Completed(runId: Id) extends CommandResponse(Positive)

  /**
   * Represents a negative response that states that a command is no longer valid
   *
   * @param runId of command for which this response is created
   * @param issue describing the cause of invalidation
   */
  case class NoLongerValid(runId: Id, issue: CommandIssue) extends CommandResponse(Negative)

  /**
   * Represents a negative response that describes an error in executing the command
   *
   * @param runId of command for which this response is created
   * @param message describing the reason or cause or action item of the error encountered while executing the command
   */
  case class Error(runId: Id, message: String) extends CommandResponse(Negative)

  /**
   * Represents a negative response that describes the cancellation of command
   *
   * @param runId of command for which this response is created
   */
  case class Cancelled(runId: Id) extends CommandResponse(Negative)

  /**
   * Represents a negative response stating that a command is not available
   *
   * @param runId of command for which this response is created
   */
  case class CommandNotAvailable(runId: Id) extends CommandResponse(Negative)

  /**
   * Represents a negative response stating that a command is not allowed
   *
   * @param runId of command for which this response is created
   * @param issue describing the cause of invalidation
   */
  case class NotAllowed(runId: Id, issue: CommandIssue) extends CommandResponse(Negative)

  /**
   * Transform a given CommandResponse to a response with the provided Id
   *
   * @param id the RunId for the new CommandResponse
   * @param commandResponse the CommandResponse to be transformed
   * @return a CommandResponse that has runId as provided id
   */
  def withRunId(id: Id, commandResponse: CommandResponse): CommandResponse = commandResponse match {
    //case accepted: Accepted                       ⇒ accepted.copy(runId = id)
    //case invalid: Invalid                         ⇒ invalid.copy(runId = id)
    case completedWithResult: CompletedWithResult ⇒ completedWithResult.copy(runId = id)
    case completed: Completed                     ⇒ completed.copy(runId = id)
    case noLongerValid: NoLongerValid             ⇒ noLongerValid.copy(runId = id)
    case error: Error                             ⇒ error.copy(runId = id)
    case cancelled: Cancelled                     ⇒ cancelled.copy(runId = id)
    case commandNotAvailable: CommandNotAvailable ⇒ commandNotAvailable.copy(runId = id)
    case notAllowed: NotAllowed                   ⇒ notAllowed.copy(runId = id)
  }

  /**
   * Creates an aggregated response from a collection of CommandResponses received from other components. If one of the
   * CommandResponses fail, the aggregated response fails and further processing of any more CommandResponse is terminated.
   *
   * @param commandResponses a stream of CommandResponses
   * @return a future of aggregated response
   */
  def aggregateResponse(
      commandResponses: Source[CommandResponseBase, NotUsed]
  )(implicit ec: ExecutionContext, mat: Materializer): Future[CommandResponseBase] = {
    commandResponses
      .runForeach { x ⇒
        if (x.resultType == CommandResultType.Negative)
          throw new RuntimeException(s"Command with runId [${x.runId}] failed with response [$x]")
      }
      .transform {
        case Success(_)  ⇒ Success(CommandResponse.Completed(Id()))
        case Failure(ex) ⇒ Success(CommandResponse.Error(Id(), s"${ex.getMessage}"))
      }
  }
}

/**
 * The nature of CommandResponse as an intermediate response of command execution or a final response which could be
 * positive or negative
 */
sealed trait CommandResultType extends EnumEntry
object CommandResultType extends Enum[CommandResultType] {

  override def values: immutable.IndexedSeq[CommandResultType] = findValues

  /**
   * A CommandResponse of intermediate type
   */
  case object Intermediate extends CommandResultType

  /**
   * A CommandResponse of final type. It could be Positive or Negative
   */
  sealed trait Final extends CommandResultType

  /**
   * A Positive CommandResponse of Final type
   */
  case object Positive extends Final

  /**
   * A Negative CommandResponse of Final type
   */
  case object Negative extends Final

}
