package csw.location.server.http

import csw.location.api.codec.LocationServiceCodecs._
import csw.location.api.messages.LocationStreamingRequest
import csw.location.api.messages.LocationStreamingRequest.Track
import csw.location.api.scaladsl.LocationService
import msocket.api.{StreamRequestHandler, StreamResponse}

class LocationStreamingRequestHandler(locationService: LocationService) extends StreamRequestHandler[LocationStreamingRequest] {
  override def handle(request: LocationStreamingRequest): StreamResponse =
    request match {
      case Track(connection) => stream(locationService.track(connection))
    }
}
