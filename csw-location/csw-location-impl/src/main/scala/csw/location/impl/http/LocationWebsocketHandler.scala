package csw.location.impl.http

import akka.NotUsed
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.Source
import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.messages.LocationWebsocketMessage
import csw.location.api.messages.LocationWebsocketMessage.Track
import csw.location.api.scaladsl.LocationService
import msocket.api.Encoding
import msocket.impl.ws.WebsocketHandler
import LocationServiceCodecs._

class LocationWebsocketHandler(locationService: LocationService, encoding: Encoding[_])
    extends WebsocketHandler[LocationWebsocketMessage](encoding) {
  override def handle(request: LocationWebsocketMessage): Source[Message, NotUsed] = request match {
    case Track(connection) => stream(locationService.track(connection))
  }
}
