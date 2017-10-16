package csw.services.tracklocation.common

import java.net.URI

import csw.messages.location.Connection.TcpConnection
import csw.messages.location.TcpLocation

object LocationFactory {
  def tcp(connection: TcpConnection, uri: URI) = TcpLocation(connection, uri, null)
}
