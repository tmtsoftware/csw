package csw.location.server.http

import akka.NotUsed
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.Source
import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.messages.LocationWebsocketMessage
import csw.location.api.messages.LocationWebsocketMessage.Track
import csw.location.api.scaladsl.LocationService
import msocket.api.MessageHandler
import msocket.impl.Encoding
import msocket.impl.ws.WebsocketStreamExtensions

class LocationWebsocketHandler(locationService: LocationService, val encoding: Encoding[_])
    extends MessageHandler[LocationWebsocketMessage, Source[Message, NotUsed]]
    with LocationServiceCodecs
    with WebsocketStreamExtensions {
  override def handle(request: LocationWebsocketMessage): Source[Message, NotUsed] = request match {
    case Track(connection) => stream(locationService.track(connection))
  }
}
