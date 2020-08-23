package csw.command.client.handlers

import csw.command.api.codecs.CommandServiceCodecs._
import csw.command.api.messages.CommandServiceStreamingRequest
import csw.command.api.messages.CommandServiceStreamingRequest._
import csw.command.api.scaladsl.CommandService
import msocket.api.{StreamRequestHandler, StreamResponse}

import scala.concurrent.Future

class CommandServiceStreamingRequestHandler(commandService: CommandService)
    extends StreamRequestHandler[CommandServiceStreamingRequest] {

  override def handle(request: CommandServiceStreamingRequest): Future[StreamResponse] =
    request match {
      case QueryFinal(runId, timeout)   => future(commandService.queryFinal(runId)(timeout))
      case SubscribeCurrentState(names) => stream(commandService.subscribeCurrentState(names))
    }
}
