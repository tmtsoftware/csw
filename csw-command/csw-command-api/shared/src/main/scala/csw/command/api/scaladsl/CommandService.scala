/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.command.api.scaladsl

import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.Timeout
import csw.command.api.StateMatcher
import csw.params.commands.CommandResponse.*
import csw.params.commands.ControlCommand
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}
import csw.time.core.models.UTCTime
import msocket.api.Subscription

import scala.concurrent.Future

/**
 * A Command Service API of a csw component. This model provides method based APIs for command interactions with a component.
 */
trait CommandService {

  /**
   * Send a Validate command and get ValidateResponse as a Future. The ValidateResponse can be of type Accepted, Invalid
   * or Locked.
   *
   * @param controlCommand the [[csw.params.commands.ControlCommand]] payload
   * @return a ValidateResponse as a Future value
   */
  def validate(controlCommand: ControlCommand): Future[ValidateResponse]

  /**
   * Submit given command and returns [[csw.params.commands.CommandResponse.SubmitResponse]] as a Future
   *
   * @param controlCommand the [[csw.params.commands.ControlCommand]] payload
   * @return a SubmitResponse as a Future value
   */
  def submit(controlCommand: ControlCommand): Future[SubmitResponse]

  /**
   * Submit a command and Subscribe for the result if it was successfully validated as `Started` to get a
   * final [[csw.params.commands.CommandResponse.SubmitResponse]] as a Future
   *
   * @param controlCommand the [[csw.params.commands.ControlCommand]] payload
   * @param timeout        max-time to wait for a final response
   * @return a SubmitResponse as a Future value
   */
  def submitAndWait(controlCommand: ControlCommand)(implicit timeout: Timeout): Future[SubmitResponse]

  /**
   * Submit multiple commands and get a List of [[csw.params.commands.CommandResponse.SubmitResponse]] for all commands. The CommandResponse can be a response
   * of validation (Accepted, Invalid) or a final Response. In case of response as `Accepted`, final CommandResponse can be obtained by using `subscribe` API.
   *
   * @param submitCommands the set of [[csw.params.commands.ControlCommand]] payloads
   * @param timeout        max-time to wait for a final response
   * @return a future list of SubmitResponse, one for each command
   */
  def submitAllAndWait(submitCommands: List[ControlCommand])(implicit timeout: Timeout): Future[List[SubmitResponse]]

  /**
   * Send a command as a Oneway and get a [[csw.params.commands.CommandResponse.OnewayResponse]] as a Future.
   * The OnewayResponse can be a response of validation (Accepted, Invalid) or Locked.
   * The use of oneway is the highest performance command interaction. It is used when completion, if needed at all,
   * is provided through CurrentState or status values and Event Service.  See also [[onewayAndMatch]]
   *
   * @param controlCommand the [[csw.params.commands.ControlCommand]] payload
   * @return a OnewayResponse as a Future value
   */
  def oneway(controlCommand: ControlCommand): Future[OnewayResponse]

  /**
   * Submit a command and match the published state from the component using a [[csw.command.api.StateMatcher]].
   * If the match is successful a `Completed` response is
   * provided as a future. In case of a failure or unmatched state, `Error` CommandResponse is provided as a Future.
   *
   * @param controlCommand the [[csw.params.commands.ControlCommand]] payload
   * @param stateMatcher   the StateMatcher implementation for matching received state against a demand state
   * @return a MatchingResponse as a Future value
   */
  def onewayAndMatch(controlCommand: ControlCommand, stateMatcher: StateMatcher): Future[MatchingResponse]

  /**
   * Query for the result of a long running command which was sent as Submit to get a [[csw.params.commands.CommandResponse.SubmitResponse]] as a Future.
   * Query allows checking to see if a long-running command is completed without waiting as with [[queryFinal]].
   *
   * @param commandRunId the runId of the command for which response is required
   * @return a SubmitResponse as a Future value
   */
  def query(commandRunId: Id): Future[SubmitResponse]

  /**
   * Query for the final result of a long running command which was sent as Submit to get a [[csw.params.commands.CommandResponse.SubmitResponse]] as a Future
   *
   * @param commandRunId the runId of the command for which response is required
   * @param timeout      max-time to wait for a final response
   * @return a SubmitResponse as a Future value
   */
  def queryFinal(commandRunId: Id)(implicit timeout: Timeout): Future[SubmitResponse]

  /**
   * Subscribe to the current state of a component corresponding to the [[csw.location.api.models.PekkoLocation]] of the component
   *
   * @param names subscribe to states which have any of the provided value for name.
   *              If no states are provided, all the current states will be received.
   * @return a stream of current states with CurrentStateSubscription as the materialized value which can be used to stop the subscription
   */
  def subscribeCurrentState(names: Set[StateName] = Set.empty): Source[CurrentState, Subscription]

  /**
   * Subscribe to the current state of a component corresponding to the [[csw.location.api.models.PekkoLocation]] of the component
   *
   * @note Callbacks are not thread-safe on the JVM. If you are doing side effects/mutations inside the callback, you should ensure that it is done in a thread-safe way inside an actor.
   * @param callback the action to be applied on the CurrentState element received as a result of subscription
   * @return a Subscription to stop the subscription
   */
  def subscribeCurrentState(callback: CurrentState => Unit): Subscription

  /**
   * Subscribe to the current state of a component corresponding to the [[csw.location.api.models.PekkoLocation]] of the component
   *
   * @note Callbacks are not thread-safe on the JVM. If you are doing side effects/mutations inside the callback, you should ensure that it is done in a thread-safe way inside an actor.
   * @param names    subscribe to only those states which have any of the provided value for name
   * @param callback the action to be applied on the CurrentState element received as a result of subscription
   * @return a Subscription to stop the subscription
   */
  def subscribeCurrentState(names: Set[StateName], callback: CurrentState => Unit): Subscription

  /**
   * On receiving a diagnostic data command, the component goes into a diagnostic data mode based on hint at the specified startTime.
   * Validation of supported hints need to be handled by the component writer.
   *
   * @param startTime represents the time at which the diagnostic mode actions will take effect
   * @param hint      represents supported diagnostic data mode for a component
   */
  def executeDiagnosticMode(startTime: UTCTime, hint: String): Unit

  /**
   * On receiving a operations mode command, the current diagnostic data mode is halted.
   */
  def executeOperationsMode(): Unit

  /**
   * A component can be notified to run in online mode again in case it was put to run in offline mode.
   */
  def onGoOnline(): Unit

  /**
   * A component can be notified to run in offline mode.
   */
  def onGoOffline(): Unit
}
