package csw.params.commands

import csw.params.core.models.Id
import csw.serializable.TMTSerializable

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

  /**
   * ValidationResponse is returned by validateCommand handler.
   * Values can only be Invalid, Accepted
   */
  sealed trait ValidationResponse extends Response

  /**
   * ValidateOnlyResponse is returned by Validate message, which calls validateCommand handler
   * Values can be Invalid, Accepted, Locked.
   * Since the component can be locked, it is ValidationResponse with Locked
   */
  sealed trait ValidateOnlyResponse extends Response

  /**
   * OnewayResponse is returned by Oneway message which calls the onOneway handler
   * Responses returned can be Accepted, Invalid, Locked
   */
  sealed trait OnewayResponse extends Response

  /**
   * SubmitResponse is returned by Submit message which calls the onSubmit handler
   * Responses returned can be Invalid, Started, Completed, CompletedWithResult, Error, Cancelled, Locked
   */
  sealed trait SubmitResponse extends QueryResponse

  /**
   * QueryResponse is returned by CommandService query
   * Values can be Invalid, Started, Completed, CompletedWithResult, Error, Cancelled, Locked, CommandNotAvailable
   */
  sealed trait QueryResponse extends Response

  /**
   * MatchingResponse is returned by matchers.
   * Responses returned can be Invalid, Completed, Error, Locked
   */
  sealed trait MatchingResponse extends Response

  /**
   * Represents a final response stating acceptance of a command received
   *
   * @param runId the runId of command for which this response is created
   */
  case class Accepted(runId: Id) extends ValidationResponse with ValidateOnlyResponse with OnewayResponse

  /**
   * Represents an intermediate response stating a long running command has been started
   *
   * @param runId of command for which this response is created
   */
  case class Started(runId: Id) extends SubmitResponse

  /**
   * Represents a positive response stating completion of command
   *
   * @param runId of command for which this response is created
   * @param result describing the result of completion
   */
  case class CompletedWithResult(runId: Id, result: Result) extends SubmitResponse

  /**
   * Represents a positive response stating completion of command
   *
   * @param runId of command for which this response is created
   */
  case class Completed(runId: Id) extends SubmitResponse with MatchingResponse

  /**
   * Represents a negative response invalidating a command received
   *
   * @param runId of command for which this response is created
   * @param issue describing the cause of invalidation
   */
  case class Invalid(runId: Id, issue: CommandIssue)
      extends ValidationResponse
      with ValidateOnlyResponse
      with OnewayResponse
      with SubmitResponse
      with MatchingResponse

  /**
   * Represents a negative response that describes an error in executing the command
   *
   * @param runId of command for which this response is created
   * @param message describing the reason or cause or action item of the error encountered while executing the command
   */
  case class Error(runId: Id, message: String) extends SubmitResponse with MatchingResponse

  /**
   * Represents a negative response that describes the cancellation of command
   *
   * @param runId of command for which this response is created
   */
  case class Cancelled(runId: Id) extends SubmitResponse

  /**
   * Represents a negative response stating that a component is Locked and command was not validated or executed
   *
   * @param runId of command for which this response is created
   */
  case class Locked(runId: Id) extends ValidateOnlyResponse with OnewayResponse with SubmitResponse with MatchingResponse

  /**
   * A negative response stating that a command with given runId is not available or cannot be located
   *
   * @param runId of command for which this response is created
   */
  case class CommandNotAvailable(runId: Id) extends QueryResponse

  /**
   * Transform a given CommandResponse to a response with the provided Id
   *
   * @param id       the RunId for the new CommandResponse
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
  }

  /**
   * Tests a response to determine if it is a final command state
   * @param qr response for testing
   * @return true if it is final
   */
  def isFinal(qr: QueryResponse): Boolean = qr match {
    case Started(_) => false
    case _          => true
  }

  /**
   * Tests a response to determine if it is a positive response
   * @param qr response for testing
   * @return true if it is positive
   */
  def isPositive(qr: QueryResponse): Boolean = qr match {
    case Completed(_) | CompletedWithResult(_, _) => true
    case _                                        => false
  }

  /**
   * Tests a response to determine if it is a negative response
   * @param qr response for testing
   * @return true if it is negative
   */
  def isNegative(qr: QueryResponse): Boolean = !(isPositive(qr) || isIntermediate(qr))

  /**
   * Tests a response to determine if it is an intermediate response
   * @param qr response for testing
   * @return returns true if it is intermediate
   */
  def isIntermediate(qr: QueryResponse): Boolean = qr match {
    case Started(_) => true
    case _          => false
  }

}
