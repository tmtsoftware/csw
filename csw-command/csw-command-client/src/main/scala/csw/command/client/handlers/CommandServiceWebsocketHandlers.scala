package csw.command.client.handlers

import akka.NotUsed
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.Source
import csw.command.api.codecs.CommandServiceCodecs._
import csw.command.api.messages.CommandServiceWebsocketMessage
import csw.command.api.messages.CommandServiceWebsocketMessage._
import csw.command.api.scaladsl.CommandService
import msocket.api.ContentType
import msocket.impl.ws.WebsocketHandler

class CommandServiceWebsocketHandlers(commandService: CommandService, contentType: ContentType)
    extends WebsocketHandler[CommandServiceWebsocketMessage](contentType) {

  override def handle(request: CommandServiceWebsocketMessage): Source[Message, NotUsed] = request match {
    case QueryFinal(runId, timeout)   => futureAsStream(commandService.queryFinal(runId)(timeout))
    case SubscribeCurrentState(names) => stream(commandService.subscribeCurrentState(names))
  }
}
