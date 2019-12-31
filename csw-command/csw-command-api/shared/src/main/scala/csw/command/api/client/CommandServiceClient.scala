package csw.command.api.client

import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.Source
import akka.util.Timeout
import csw.command.api.StateMatcher
import csw.command.api.codecs.CommandServiceCodecs
import csw.command.api.messages.CommandServiceHttpMessage._
import csw.command.api.messages.CommandServiceWebsocketMessage._
import csw.command.api.messages.{CommandServiceHttpMessage, CommandServiceWebsocketMessage}
import csw.command.api.scaladsl.CommandService
import csw.command.api.utils.CommandServiceExtension
import csw.params.commands.CommandResponse._
import csw.params.commands.ControlCommand
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}
import msocket.api.{Subscription, Transport}

import scala.concurrent.Future

class CommandServiceClient(
    httpTransport: Transport[CommandServiceHttpMessage],
    websocketTransport: Transport[CommandServiceWebsocketMessage]
)(implicit actorSystem: ActorSystem[_])
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

  override def subscribeCurrentState(callback: CurrentState => Unit): Subscription =
    websocketTransport.requestStream[CurrentState](SubscribeCurrentState(), callback)

  override def subscribeCurrentState(names: Set[StateName], callback: CurrentState => Unit): Subscription =
    websocketTransport.requestStream[CurrentState](SubscribeCurrentState(names), callback)
}
