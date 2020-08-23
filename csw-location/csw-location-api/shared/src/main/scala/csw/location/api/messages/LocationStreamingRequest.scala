package csw.location.api.messages

import csw.location.api.models.Connection

sealed trait LocationStreamingRequest

object LocationStreamingRequest {
  case class Track(connection: Connection) extends LocationStreamingRequest
}
