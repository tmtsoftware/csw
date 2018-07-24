package csw.params.commands

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import csw.messages.TMTSerializable
import csw.messages.commands.CommandResultType.{Intermediate, Negative, Positive}
import csw.messages.params.models.Id
import enumeratum._

import scala.collection.immutable

/**
 * The nature of CommandResponse as an intermediate response of command execution or a final response which could be
 * positive or negative
 */
/*
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
 */

sealed trait CommandResultType
object CommandResultType {

  case object Intermediate extends CommandResultType

  sealed trait Final extends CommandResultType

  case object Negative extends Final

  case object Positive extends Final

}

object Responses {

  import CommandResultType._

  sealed trait Response extends TMTSerializable {
    def resultType: CommandResultType

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

  case class Accepted(runId: Id) extends ValidationResponse with OnewayResponse {
    val resultType: CommandResultType = Positive
  }

  case class Started(runId: Id) extends SubmitResponse {
    val resultType: CommandResultType = Intermediate
  }

  case class CompletedWithResult(runId: Id, result: Result) extends SubmitResponse {
    val resultType: CommandResultType = Positive
  }

  case class Completed(runId: Id) extends SubmitResponse {
    val resultType: CommandResultType = Positive
  }

  case class Invalid(runId: Id, issue: CommandIssue) extends ValidationResponse with OnewayResponse with SubmitResponse {
    val resultType: CommandResultType = Negative
  }

  case class Error(runId: Id, message: String) extends SubmitResponse {
    val resultType: CommandResultType = Negative
  }

  case class Cancelled(runId: Id) extends SubmitResponse {
    val resultType: CommandResultType = Negative
  }

  case class Locked(runId: Id) extends OnewayResponse with SubmitResponse {
    val resultType: CommandResultType = Negative
  }

  case class CommandNotAvailable(runId: Id) extends SubmitResponse {
    val resultType: CommandResultType = Negative
  }

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
        if (x.resultType == Negative)
          throw new RuntimeException(s"Command with runId [${x.runId}] failed with response [$x]")
      }
      .transform {
        case Success(_)  ⇒ Success(Responses.Completed(Id()))
        case Failure(ex) ⇒ Success(Responses.Error(Id(), s"${ex.getMessage}"))
      }
  }
  /*
  def validate():ValidationResponse = {
    Accepted(Id())
    Invalid(Id(), WrongInternalStateIssue(""))
    //Started(Id())
  }

  def oneway():OnewayResponse = {
    Invalid(Id(), WrongInternalStateIssue(""))
    Accepted(Id())
  }

  def submit():SubmitResponse = {
    Invalid(Id(), WrongInternalStateIssue(""))
    Completed(Id())
    Started(Id())
    Locked(Id())
    Error(Id(), "Bogus")
    Cancelled(Id())
    CompletedWithResult(Id(), Result(Prefix("wfos")))
  }
 */
}
