package csw.messages.ccs.commands

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import csw.messages.TMTSerializable
import csw.messages.ccs.CommandIssue
import csw.messages.ccs.commands.CommandResultType.{Intermediate, Negative, Positive}
import csw.messages.params.models.RunId

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * Parent type of a response of command Execution
 * @param resultType the nature of command response as [[csw.messages.ccs.commands.CommandResultType]]
 */
sealed abstract class CommandResponse(val resultType: CommandResultType) extends TMTSerializable {
  def runId: RunId
}

object CommandResponse {
  case class Accepted(runId: RunId)                            extends CommandResponse(Intermediate)
  case class Invalid(runId: RunId, issue: CommandIssue)        extends CommandResponse(Negative)
  case class CompletedWithResult(runId: RunId, result: Result) extends CommandResponse(Positive)
  case class Completed(runId: RunId)                           extends CommandResponse(Positive)
  case class NoLongerValid(runId: RunId, issue: CommandIssue)  extends CommandResponse(Negative)
  case class Error(runId: RunId, message: String)              extends CommandResponse(Negative)
  case class Cancelled(runId: RunId)                           extends CommandResponse(Negative)
  case class CommandNotAvailable(runId: RunId)                 extends CommandResponse(Negative)
  case class NotAllowed(runId: RunId, issue: CommandIssue)     extends CommandResponse(Negative)

  /**
   * Transform a given CommandResponse to a response with the provided RunId
   * @param id the RunId for the new CommandResponse
   * @param commandResponse the CommandResponse to be transformed
   * @return
   */
  def withRunId(id: RunId, commandResponse: CommandResponse): CommandResponse = commandResponse match {
    case accepted: Accepted                       ⇒ accepted.copy(runId = id)
    case invalid: Invalid                         ⇒ invalid.copy(runId = id)
    case completedWithResult: CompletedWithResult ⇒ completedWithResult.copy(runId = id)
    case completed: Completed                     ⇒ completed.copy(runId = id)
    case noLongerValid: NoLongerValid             ⇒ noLongerValid.copy(runId = id)
    case error: Error                             ⇒ error.copy(runId = id)
    case cancelled: Cancelled                     ⇒ cancelled.copy(runId = id)
    case commandNotAvailable: CommandNotAvailable ⇒ commandNotAvailable.copy(runId = id)
    case notAllowed: NotAllowed                   ⇒ notAllowed.copy(runId = id)
  }

  /**
   * Creates an aggregate response from a stream of CommandResponse.
   * @param commandResponses a source of stream of CommandResponse
   * @return
   */
  def aggregateResponse(
      commandResponses: Source[CommandResponse, NotUsed]
  )(implicit ec: ExecutionContext, mat: Materializer): Future[CommandResponse] = {
    commandResponses
      .runForeach { x ⇒
        if (x.resultType == CommandResultType.Negative)
          throw new RuntimeException(s"Command with runId [${x.runId}] failed with response [$x]")
      }
      .transform {
        case Success(_)  ⇒ Success(CommandResponse.Completed(RunId()))
        case Failure(ex) ⇒ Success(CommandResponse.Error(RunId(), s"${ex.getMessage}"))
      }
  }
}

/**
 * The nature of CommandResponse as an intermediate response of command execution or a final response which could be
 * positive or negative
 */
sealed trait CommandResultType
object CommandResultType {
  case object Intermediate extends CommandResultType
  sealed trait Final       extends CommandResultType
  case object Positive     extends Final
  case object Negative     extends Final
}
