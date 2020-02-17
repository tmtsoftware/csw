package csw.location.api.messages

import csw.location.api.models.Connection

sealed trait LocationWebsocketMessage

object LocationWebsocketMessage {
  case class Track(connection: Connection) extends LocationWebsocketMessage
}
