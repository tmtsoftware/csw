package csw.params.commands

import csw.params.core.models.Id
import csw.serializable.CommandSerializable

sealed trait CommandResponse extends CommandSerializable {

  /**
   * A helper method to get the runId for this command response
   *
   * @return the runId of command for which this response is created
   */
  def runId: Id
}

/**
 * The nature of CommandResponse as an intermediate response of command execution or a final response which could be
 * positive or negative
 */
object CommandResponse {

  /**
   * ValidationResponse is returned by validateCommand handler.
   * Values can only be Invalid, Accepted
   */
  sealed trait ValidateCommandResponse extends CommandResponse

  /**
   * ValidateOnlyResponse is returned by Validate message, which calls validateCommand handler
   * Values can be Invalid, Accepted, Locked.
   * Since the component can be locked, it is ValidationResponse with Locked
   */
  sealed trait ValidateResponse extends CommandResponse

  /**
   * OnewayResponse is returned by Oneway message which calls the onOneway handler
   * Responses returned can be Accepted, Invalid, Locked
   */
  sealed trait OnewayResponse extends CommandResponse

  /**
   * SubmitResponse is returned by Submit message which calls the onSubmit handler
   * Responses returned can be Invalid, Started, Completed, Error, Cancelled, Locked
   */
  sealed trait SubmitResponse extends CommandResponse {
    def typeName: String = this.getClass.getSimpleName
    def withRunId(runId: Id): SubmitResponse
  }

  /**
   * MatchingResponse is returned by matchers.
   * Responses returned can be Invalid, Completed, Error, Locked
   */
  sealed trait MatchingResponse extends CommandResponse

  /**
   * Represents a final response stating acceptance of a command received meaning passed validation
   *
   * @param runId the runId of command for which this response is created
   */
  case class Accepted(runId: Id) extends ValidateCommandResponse with ValidateResponse with OnewayResponse

  /**
   * Represents a preliminary response stating a long running command has been started
   *
   * @param runId of command for which this response is created
   */
  case class Started(runId: Id) extends SubmitResponse {
    def withRunId(newRunId: Id): Started = copy(runId = newRunId)
  }

  /**
   * Represents a final positive response stating completion of command with no errors
   * A result may be included or may be empty
   *
   * @param runId of command for which this response is created
   * @param result describing the result of completion if needed
   */
  case class Completed(runId: Id, result: Result) extends SubmitResponse with MatchingResponse {

    /**
     * Check to see if this response has a result
     * @return `true` if the response contains a non-empty result, `false` otherwise.
     */
    def hasResult: Boolean = result.nonEmpty

    /**
     * A java helper to construct a Completed response
     */
    def this(runId: Id) = this(runId, Result())

    def withRunId(newRunId: Id): Completed = copy(runId = newRunId)
  }

  object Completed {
    def apply(runId: Id): Completed = new Completed(runId, Result.emptyResult)
  }

  /**
   * Represents a final negative response invalidating a command received has failed validation
   *
   * @param runId of command for which this response is created
   * @param issue describing the cause of invalidation
   */
  case class Invalid(runId: Id, issue: CommandIssue)
      extends ValidateCommandResponse
      with ValidateResponse
      with OnewayResponse
      with SubmitResponse
      with MatchingResponse {

    def withRunId(newRunId: Id): Invalid = copy(runId = newRunId)
  }

  /**
   * Represents a negative response that describes an error in executing the command
   *
   * @param runId of command for which this response is created
   * @param message describing the reason or cause or action item of the error encountered while executing the command
   */
  case class Error(runId: Id, message: String) extends SubmitResponse with MatchingResponse {

    def withRunId(newRunId: Id): Error = copy(runId = newRunId)
  }

  /**
   * Represents a negative response that describes the cancellation of command
   *
   * @param runId of command for which this response is created
   */
  case class Cancelled(runId: Id) extends SubmitResponse {

    def withRunId(newRunId: Id): Cancelled = copy(runId = newRunId)
  }

  /**
   * Represents a negative response stating that a component is Locked and command was not validated or executed
   *
   * @param runId of command for which this response is created
   */
  case class Locked(runId: Id) extends ValidateResponse with OnewayResponse with SubmitResponse with MatchingResponse {

    def withRunId(newRunId: Id): Locked = copy(runId = newRunId)
  }

  /**
   * Tests a response to determine if it is a final command state
   *
   * @param sr response for testing
   * @return true if it is final
   */
  def isFinal(sr: SubmitResponse): Boolean =
    sr match {
      case Started(_) => false
      case _          => true
    }

  /**
   * Test a response to determine if it is a positive response
   *
   * @param sr response for testing
   * @return true if it is positive
   */
  def isPositive(sr: SubmitResponse): Boolean =
    sr match {
      case Completed(_, _) => true
      case _               => false
    }

  /**
   * Test a OnewayResponse to determine if it is a positive response
   *
   * @param or a OnewayResponse for testing
   * @return true if positive, false otherwise
   */
  def isPositive(or: OnewayResponse): Boolean =
    or match {
      case Accepted(_) => true
      case _           => false
    }

  /**
   * Tests a response to determine if it is a negative response
   *
   * @param sr response for testing
   * @return true if it is negative
   */
  def isNegative(sr: SubmitResponse): Boolean = !(isPositive(sr) || isIntermediate(sr))

  /**
   * Tests a response to determine if it is an intermediate response
   *
   * @param sr response for testing
   * @return returns true if it is intermediate
   */
  def isIntermediate(sr: SubmitResponse): Boolean =
    sr match {
      case Started(_) => true
      case _          => false
    }
}
