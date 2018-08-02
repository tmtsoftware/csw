package csw.params.commands

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import csw.messages.TMTSerializable
import csw.messages.commands.CommandResponse.CommandResultType.{Intermediate, Negative, Positive}
import csw.messages.params.models.Id
import enumeratum._

import scala.collection.immutable

/**
 * The nature of CommandResponse as an intermediate response of command execution or a final response which could be
 * positive or negative
 */
object CommandResponse {

  sealed trait Response extends TMTSerializable {

    /**
     * A helper method to get the runId for this command response
     *
     * @return the runId of command for which this response is created
     */
    def runId: Id
  }

  sealed trait ValidationResponse extends Response

  sealed trait OnewayResponse extends Response

  sealed trait SubmitResponse extends Response

  sealed trait MatchingResponse extends Response

  case class Accepted(runId: Id) extends ValidationResponse with OnewayResponse

  case class Started(runId: Id) extends SubmitResponse

  case class CompletedWithResult(runId: Id, result: Result) extends SubmitResponse

  case class Completed(runId: Id) extends SubmitResponse with MatchingResponse

  case class Invalid(runId: Id, issue: CommandIssue)
      extends ValidationResponse
      with OnewayResponse
      with SubmitResponse
      with MatchingResponse

  case class Error(runId: Id, message: String) extends SubmitResponse with MatchingResponse

  case class Cancelled(runId: Id) extends SubmitResponse

  case class Locked(runId: Id) extends OnewayResponse with SubmitResponse with MatchingResponse

  case class CommandNotAvailable(runId: Id) extends SubmitResponse

  /**
   * Transform a given CommandResponse to a response with the provided Id
   *
   * @param id the RunId for the new CommandResponse
   * @param response the CommandResponse to be transformed
   * @return a CommandResponse that has runId as provided id
   */
  def withRunId(id: Id, response: SubmitResponse): SubmitResponse = response match {
    case started: Started                         ⇒ started.copy(runId = id)
    case invalid: Invalid                         ⇒ invalid.copy(runId = id)
    case completedWithResult: CompletedWithResult ⇒ completedWithResult.copy(runId = id)
    case completed: Completed                     ⇒ completed.copy(runId = id)
    case locked: Locked                           ⇒ locked.copy(runId = id)
    case error: Error                             ⇒ error.copy(runId = id)
    case cancelled: Cancelled                     ⇒ cancelled.copy(runId = id)
    case commandNotAvailable: CommandNotAvailable ⇒ commandNotAvailable.copy(runId = id)
  }

  sealed trait CommandResultType
  object CommandResultType {

    case object Intermediate extends CommandResultType

    sealed trait Final extends CommandResultType

    case object Negative extends Final

    case object Positive extends Final

  }

  def isCommandResultType(sr: SubmitResponse): CommandResultType = sr match {
    case Completed(_) | CompletedWithResult(_, _) => Positive
    case Started(_)                               => Intermediate
    case _                                        => Negative
  }

  /**
   * Creates an aggregated response from a collection of CommandResponses received from other components. If one of the
   * CommandResponses fail, the aggregated response fails and further processing of any more CommandResponse is terminated.
   *
   * @param commandResponses a stream of CommandResponses
   * @return a future of aggregated response
   */
  def aggregateResponse(commandResponses: Source[SubmitResponse, NotUsed])(implicit ec: ExecutionContext,
                                                                           mat: Materializer): Future[SubmitResponse] = {
    commandResponses
      .runForeach { x ⇒
        if (isCommandResultType((x)) == Negative)
          throw new RuntimeException(s"Command with runId [${x.runId}] failed with response [$x]")
      }
      .transform {
        case Success(_)  ⇒ Success(Completed(Id()))
        case Failure(ex) ⇒ Success(Error(Id(), s"${ex.getMessage}"))
      }
  }

}
