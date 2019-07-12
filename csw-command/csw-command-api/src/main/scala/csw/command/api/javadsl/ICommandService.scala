package csw.command.api.javadsl
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

import akka.stream.javadsl.Source
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
   * Send a Validate command and get ValidateResponse as a Future. The ValidateResponse can be of type Accepted, Invalid
   * or Locked.
   *
   * @param controlCommand the [[csw.params.commands.ControlCommand]] payload
   * @return a ValidateResponse as a Future value
   */
  def validate(controlCommand: ControlCommand): CompletableFuture[ValidateResponse]

  /**
   * Submit given command and returns [[csw.params.commands.CommandResponse.SubmitResponse]] as a Future
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
  def submitAndWait(controlCommand: ControlCommand, timeout: Timeout): CompletableFuture[SubmitResponse]

  /**
   * Submit multiple commands and get a Source of [[csw.params.commands.CommandResponse.SubmitResponse]] for all commands. The CommandResponse can be a response
   * of validation (Accepted, Invalid) or a final Response. In case of response as `Accepted`, final CommandResponse can be obtained by using `subscribe` API.
   *
   * @param submitCommands the set of [[csw.params.commands.ControlCommand]] payloads
   * @return a Source of CommandResponse as a stream of CommandResponses for all commands
   */
  def submitAllAndWait(
      submitCommands: java.util.List[ControlCommand],
      timeout: Timeout
  ): CompletableFuture[java.util.List[SubmitResponse]]

  /**
   * Send a command as a Oneway and get a [[csw.params.commands.CommandResponse.OnewayResponse]] as a Future. The CommandResponse can be a response
   * of validation (Accepted, Invalid) or a final Response.
   *
   * @param controlCommand the [[csw.params.commands.ControlCommand]] payload
   * @return a CommandResponse as a Future value
   */
  def oneway(controlCommand: ControlCommand, timeout: Timeout): CompletableFuture[OnewayResponse]

  /**
   * Submit a command and match the published state from the component using a [[csw.command.api.StateMatcher]].
   * If the match is successful a `Completed` response is
   * provided as a future. In case of a failure or unmatched state, `Error` CommandResponse is provided as a Future.
   *
   * @param controlCommand the [[csw.params.commands.ControlCommand]] payload
   * @param stateMatcher the StateMatcher implementation for matching received state against a demand state
   * @return a MatchingResponse as a Future value
   */
  def onewayAndMatch(
      controlCommand: ControlCommand,
      stateMatcher: StateMatcher,
      timeout: Timeout
  ): CompletableFuture[MatchingResponse]

  /**
   * Query for the result of a long running command which was sent as Submit to get a [[csw.params.commands.CommandResponse.QueryResponse]] as a Future
   *
   * @param commandRunId the runId of the command for which response is required
   * @return a CommandResponse as a Future value
   */
  def query(commandRunId: Id, timeout: Timeout): CompletableFuture[QueryResponse]

  /**
   * Query for the final result of a long running command which was sent as Submit to get a [[csw.params.commands.CommandResponse.SubmitResponse]] as a Future
   *
   * @param commandRunId the runId of the command for which response is required
   * @return a CommandResponse as a Future value
   */
  def queryFinal(commandRunId: Id, timeout: Timeout): CompletableFuture[SubmitResponse]

  /**
   * Subscribe to all the current states of a component corresponding to the [[csw.location.model.scaladsl.AkkaLocation]] of the component
   *
   * @return  a stream of current states with CurrentStateSubscription as the materialized value which can be used to stop the subscription
   */
  def subscribeCurrentState(): Source[CurrentState, CurrentStateSubscription]

  /**
   * Subscribe to the current state of a component corresponding to the [[csw.location.model.scaladsl.AkkaLocation]] of the component
   *
   * @param names subscribe to states which have any of the provided value for name.
   *              If no states are provided, all the current states will be received.
   * @return  a stream of current states with CurrentStateSubscription as the materialized value which can be used to stop the subscription
   */
  def subscribeCurrentState(names: java.util.Set[StateName]): Source[CurrentState, CurrentStateSubscription]

  /**
   * Subscribe to the current state of a component corresponding to the [[csw.location.model.scaladsl.AkkaLocation]] of the component
   *
   * Note that callbacks are not thread-safe on the JVM. If you are doing side effects/mutations inside the callback, you should ensure that it is done in a thread-safe way inside an actor.
   *
   * @param callback the action to be applied on the CurrentState element received as a result of subscription
   * @return a CurrentStateSubscription to stop the subscription
   */
  def subscribeCurrentState(callback: Consumer[CurrentState]): CurrentStateSubscription

  /**
   * Subscribe to the current state of a component corresponding to the [[csw.location.model.scaladsl.AkkaLocation]] of the component
   *
   * Note that callbacks are not thread-safe on the JVM. If you are doing side effects/mutations inside the callback, you should ensure that it is done in a thread-safe way inside an actor.
   *
   * @param names subscribe to only those states which have any of the the provided value for name
   * @param callback the action to be applied on the CurrentState element received as a result of subscription
   * @return a CurrentStateSubscription to stop the subscription
   */
  def subscribeCurrentState(names: java.util.Set[StateName], callback: Consumer[CurrentState]): CurrentStateSubscription
}
