package csw.command.api.javadsl
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

import akka.util.Timeout
import csw.command.api.{CurrentStateSubscription, StateMatcher}
import csw.params.commands.CommandResponse._
import csw.params.commands.ControlCommand
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}

/**
 * A Command Service API of a csw component. This model provides method based APIs for command interactions with a component.
 */
trait ICommandService {

  /**
   * Submit a command and get a [[csw.params.commands.CommandResponse]] as a Future. The CommandResponse can be a response
   * of validation (Accepted, Invalid) or a final Response. In case of response as `Accepted`, final CommandResponse
   * can be obtained by using `subscribe` API.
   *
   * @param controlCommand the [[csw.params.commands.ControlCommand]] payload
   * @return a CommandResponse as a Future value
   */
  def submit(controlCommand: ControlCommand, timeout: Timeout): CompletableFuture[SubmitResponse]

  /**
   * Submit a command and Subscribe for the result if it was successfully validated as `Started` to get a
   * final [[csw.params.commands.CommandResponse.SubmitResponse]] as a Future
   *
   * @param controlCommand the [[csw.params.commands.ControlCommand]] payload
   * @return a CommandResponse as a Future value
   */
  def submitAndComplete(controlCommand: ControlCommand, timeout: Timeout): CompletableFuture[SubmitResponse]

  /**
   * Submit multiple commands and get a Source of [[csw.params.commands.CommandResponse]] for all commands. The CommandResponse can be a response
   * of validation (Accepted, Invalid) or a final Response. In case of response as `Accepted`, final CommandResponse can be obtained by using `subscribe` API.
   *
   * @param submitCommands the set of [[csw.params.commands.ControlCommand]] payloads
   * @return a Source of CommandResponse as a stream of CommandResponses for all commands
   */
  def submitAndCompleteAll(
      submitCommands: java.util.List[ControlCommand],
      timeout: Timeout
  ): CompletableFuture[java.util.List[SubmitResponse]]

  /**
   * Subscribe for the result of a long running command which was sent as Submit to get a [[csw.params.commands.CommandResponse]] as a Future
   *
   * @param commandRunId the runId of the command for which response is required
   * @return a CommandResponse as a Future value
   */
  def queryFinal(commandRunId: Id, timeout: Timeout): CompletableFuture[SubmitResponse]

  /**
   * Query for the result of a long running command which was sent as Submit to get a [[csw.params.commands.CommandResponse]] as a Future
   *
   * @param commandRunId the runId of the command for which response is required
   * @return a CommandResponse as a Future value
   */
  def query(commandRunId: Id, timeout: Timeout): CompletableFuture[QueryResponse]

  /**
   * Send a command as a Oneway and get a [[csw.params.commands.CommandResponse]] as a Future. The CommandResponse can be a response
   * of validation (Accepted, Invalid) or a final Response.
   *
   * @param controlCommand the [[csw.params.commands.ControlCommand]] payload
   * @return a CommandResponse as a Future value
   */
  def send(controlCommand: ControlCommand, timeout: Timeout): CompletableFuture[OnewayResponse]

  /**
   * Submit a command and match the published state from the component using a [[StateMatcher]].
   * If the match is successful a `Completed` response is
   * provided as a future. In case of a failure or unmatched state, `Error` CommandResponse is provided as a Future.
   *
   * @param controlCommand the [[csw.params.commands.ControlCommand]] payload
   * @param stateMatcher the StateMatcher implementation for matching received state against a demand state
   * @return a MatchingResponse as a Future value
   */
  def sendAndMatch(
      controlCommand: ControlCommand,
      stateMatcher: StateMatcher,
      timeout: Timeout
  ): CompletableFuture[MatchingResponse]

  /**
   * Send a Validate command and get ValidateResponse as a Future. The ValidateResponse can be of type Accepted, Invalid
   * or Locked.
   *
   * @param controlCommand the [[csw.params.commands.ControlCommand]] payload
   * @return a ValidateResponse as a Future value
   */
  def validate(controlCommand: ControlCommand, timeout: Timeout): CompletableFuture[ValidateResponse]

  /**
   * Subscribe to the current state of a component corresponding to the [[csw.location.api.models.AkkaLocation]] of the component
   *
   * @param callback the action to be applied on the CurrentState element received as a result of subscription
   * @return a CurrentStateSubscription to stop the subscription
   */
  def subscribeCurrentState(callback: Consumer[CurrentState]): CurrentStateSubscription

  /**
   * Subscribe to the current state of a component corresponding to the [[csw.location.api.models.AkkaLocation]] of the component
   *
   * @param names subscribe to only those states which have any of the the provided value for name
   * @param callback the action to be applied on the CurrentState element received as a result of subscription
   * @return a CurrentStateSubscription to stop the subscription
   */
  def subscribeCurrentState(names: java.util.Set[StateName], callback: Consumer[CurrentState]): CurrentStateSubscription
}
