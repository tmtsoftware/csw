package csw.params.commands

import csw.params.core.models.Id
import csw.params.core.states.StateName
import csw.serializable.CommandSerializable

sealed trait CommandResponse extends CommandSerializable {

  /**
   * A helper method to get the runId for this command response
   *
   * @return the runId of command for which this response is created
   */
  def runId: Id
  def commandName: CommandName
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
   * Responses returned can be Invalid, Started, Completed, CompletedWithResult, Error, Cancelled, Locked
   */
  sealed trait SubmitResponse extends QueryResponse {
    // stateName is the simple class name as a String: Completed, Error, etc.
    def stateName: StateName = StateName(getClass.getSimpleName)
  }

  /**
   * QueryResponse is returned by CommandService query
   * Values can be Invalid, Started, Completed, CompletedWithResult, Error, Cancelled, Locked, CommandNotAvailable
   */
  sealed trait QueryResponse extends CommandResponse

  /**
   * MatchingResponse is returned by matchers.
   * Responses returned can be Invalid, Completed, Error, Locked
   */
  sealed trait MatchingResponse extends CommandResponse

  /**
   * Represents a final response stating acceptance of a command received
   *
   * @param runId the runId of command for which this response is created
   */
  case class Accepted(commandName: CommandName, runId: Id)
      extends ValidateCommandResponse
      with ValidateResponse
      with OnewayResponse

  /**
   * Represents an intermediate response stating a long running command has been started
   *
   * @param runId of command for which this response is created
   */
  case class Started(commandName: CommandName, runId: Id) extends SubmitResponse

  /**
   * Represents a positive response stating completion of command
   *
   * @param runId of command for which this response is created
   * @param result describing the result of completion
   */
  case class CompletedWithResult(commandName: CommandName, runId: Id, result: Result) extends SubmitResponse

  /**
   * Represents a positive response stating completion of command
   *
   * @param runId of command for which this response is created
   */
  case class Completed(commandName: CommandName, runId: Id) extends SubmitResponse with MatchingResponse

  /**
   * Represents a negative response invalidating a command received
   *
   * @param runId of command for which this response is created
   * @param issue describing the cause of invalidation
   */
  case class Invalid(commandName: CommandName, runId: Id, issue: CommandIssue)
      extends ValidateCommandResponse
      with ValidateResponse
      with OnewayResponse
      with SubmitResponse
      with MatchingResponse

  /**
   * Represents a negative response that describes an error in executing the command
   *
   * @param runId of command for which this response is created
   * @param message describing the reason or cause or action item of the error encountered while executing the command
   */
  case class Error(commandName: CommandName, runId: Id, message: String) extends SubmitResponse with MatchingResponse

  /**
   * Represents a negative response that describes the cancellation of command
   *
   * @param runId of command for which this response is created
   */
  case class Cancelled(commandName: CommandName, runId: Id) extends SubmitResponse

  /**
   * Represents a negative response stating that a component is Locked and command was not validated or executed
   *
   * @param runId of command for which this response is created
   */
  case class Locked(commandName: CommandName, runId: Id)
      extends ValidateResponse
      with OnewayResponse
      with SubmitResponse
      with MatchingResponse

  /**
   * A negative response stating that a command with given runId is not available or cannot be located
   *
   * @param runId of command for which this response is created
   */
  case class CommandNotAvailable(commandName: CommandName, runId: Id) extends QueryResponse

  /**
   * Tests a response to determine if it is a final command state
   *
   * @param qr response for testing
   * @return true if it is final
   */
  def isFinal(qr: QueryResponse): Boolean = qr match {
    case Started(_, _) => false
    case _             => true
  }

  /**
   * Test a QueryResponse to determine if it is a positive response
   *
   * @param qr response for testing
   * @return true if it is positive
   */
  def isPositive(qr: QueryResponse): Boolean = qr match {
    case Completed(_, _) | CompletedWithResult(_, _, _) => true
    case _                                              => false
  }

  /**
   * Test a OnewayResponse to determine if it is a positive response
   *
   * @param or a OnewayResponse for testing
   * @return true if positive, false otherwise
   */
  def isPositive(or: OnewayResponse): Boolean = or match {
    case Accepted(_, _) => true
    case _              => false
  }

  /**
   * Tests a response to determine if it is a negative response
   *
   * @param qr response for testing
   * @return true if it is negative
   */
  def isNegative(qr: QueryResponse): Boolean = !(isPositive(qr) || isIntermediate(qr))

  /**
   * Tests a response to determine if it is an intermediate response
   *
   * @param qr response for testing
   * @return returns true if it is intermediate
   */
  def isIntermediate(qr: QueryResponse): Boolean = qr match {
    case Started(_, _) => true
    case _             => false
  }

  object SubmitResponse {
    implicit object NameableSubmitResponse extends Nameable[SubmitResponse] {
      override def name(state: SubmitResponse): StateName = state.stateName
    }
  }
}
