package csw.command.client.handlers

import csw.command.api.codecs.CommandServiceCodecs._
import csw.command.api.messages.CommandServiceWebsocketMessage
import csw.command.api.messages.CommandServiceWebsocketMessage._
import csw.command.api.scaladsl.CommandService
import msocket.api.{StreamRequestHandler, StreamResponse}

class CommandServiceWebsocketHandlers(commandService: CommandService)
    extends StreamRequestHandler[CommandServiceWebsocketMessage] {

  override def handle(request: CommandServiceWebsocketMessage): StreamResponse =
    request match {
      case QueryFinal(runId, timeout)   => future(commandService.queryFinal(runId)(timeout))
      case SubscribeCurrentState(names) => stream(commandService.subscribeCurrentState(names))
    }
}
