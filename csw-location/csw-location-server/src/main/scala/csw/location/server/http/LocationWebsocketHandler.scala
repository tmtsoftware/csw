package csw.location.server.http

import csw.location.api.codec.LocationServiceCodecs._
import csw.location.api.messages.LocationWebsocketMessage
import csw.location.api.messages.LocationWebsocketMessage.Track
import csw.location.api.scaladsl.LocationService
import msocket.api.{StreamRequestHandler, StreamResponse}

class LocationWebsocketHandler(locationService: LocationService) extends StreamRequestHandler[LocationWebsocketMessage] {
  override def handle(request: LocationWebsocketMessage): StreamResponse =
    request match {
      case Track(connection) => stream(locationService.track(connection))
    }
}
