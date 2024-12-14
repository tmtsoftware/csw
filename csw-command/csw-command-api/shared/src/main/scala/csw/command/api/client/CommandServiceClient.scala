/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.command.api.client

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.Timeout
import csw.command.api.StateMatcher
import csw.command.api.codecs.CommandServiceCodecs
import csw.command.api.messages.CommandServiceRequest.*
import csw.command.api.messages.CommandServiceStreamRequest.*
import csw.command.api.messages.{CommandServiceRequest, CommandServiceStreamRequest}
import csw.command.api.scaladsl.CommandService
import csw.command.api.utils.CommandServiceExtension
import csw.params.commands.CommandResponse.*
import csw.params.commands.ControlCommand
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}
import csw.time.core.models.UTCTime
import msocket.api.{Subscription, Transport}
import msocket.portable.Observer
import org.apache.pekko.Done

import scala.concurrent.Future

class CommandServiceClient(
    httpTransport: Transport[CommandServiceRequest],
    websocketTransport: Transport[CommandServiceStreamRequest]
)(implicit actorSystem: ActorSystem[?])
    extends CommandService
    with CommandServiceCodecs {

  private val extension = new CommandServiceExtension(this)

  override def validate(controlCommand: ControlCommand): Future[ValidateResponse] =
    httpTransport.requestResponse[ValidateResponse](Validate(controlCommand))

  override def submit(controlCommand: ControlCommand): Future[SubmitResponse] =
    httpTransport.requestResponse[SubmitResponse](Submit(controlCommand))

  override def submitAndWait(controlCommand: ControlCommand)(implicit timeout: Timeout): Future[SubmitResponse] =
    extension.submitAndWait(controlCommand)

  override def submitAllAndWait(submitCommands: List[ControlCommand])(implicit timeout: Timeout): Future[List[SubmitResponse]] =
    extension.submitAllAndWait(submitCommands)

  override def oneway(controlCommand: ControlCommand): Future[OnewayResponse] =
    httpTransport.requestResponse[OnewayResponse](Oneway(controlCommand))

  override def onewayAndMatch(controlCommand: ControlCommand, stateMatcher: StateMatcher): Future[MatchingResponse] =
    extension.onewayAndMatch(controlCommand, stateMatcher)

  override def query(commandRunId: Id): Future[SubmitResponse] =
    httpTransport.requestResponse[SubmitResponse](Query(commandRunId))

  override def queryFinal(commandRunId: Id)(implicit timeout: Timeout): Future[SubmitResponse] =
    websocketTransport.requestResponse[SubmitResponse](QueryFinal(commandRunId, timeout), timeout.duration)

  override def subscribeCurrentState(names: Set[StateName]): Source[CurrentState, Subscription] =
    websocketTransport.requestStream[CurrentState](SubscribeCurrentState(names))

  override def subscribeCurrentState(callback: CurrentState => Unit): Subscription = {
    websocketTransport.requestStream[CurrentState](SubscribeCurrentState(), Observer.create(callback))
  }

  override def subscribeCurrentState(names: Set[StateName], callback: CurrentState => Unit): Subscription =
    websocketTransport.requestStream[CurrentState](SubscribeCurrentState(names), Observer.create(callback))

  override def executeDiagnosticMode(startTime: UTCTime, hint: String): Unit =
    httpTransport.requestResponse[Unit](ExecuteDiagnosticMode(startTime, hint))

  override def executeOperationsMode(): Unit =
    httpTransport.requestResponse[Unit](ExecuteOperationsMode())

  override def onGoOnline(): Unit =
    httpTransport.requestResponse[Unit](GoOnline())

  override def onGoOffline(): Unit =
    httpTransport.requestResponse[Unit](GoOffline())
}
