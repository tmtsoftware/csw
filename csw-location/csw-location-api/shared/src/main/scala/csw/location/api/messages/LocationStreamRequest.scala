package csw.location.api.messages

import csw.location.api.models.Connection

sealed trait LocationStreamRequest

object LocationStreamRequest {
  case class Track(connection: Connection) extends LocationStreamRequest
}
