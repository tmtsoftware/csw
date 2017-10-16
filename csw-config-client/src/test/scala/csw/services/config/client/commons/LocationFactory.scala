package csw.services.config.client.commons

import java.net.URI

import csw.messages.location.Connection.HttpConnection
import csw.messages.location.HttpLocation

object LocationFactory {
  def http(connection: HttpConnection, uri: URI) = HttpLocation(connection, uri, null)
}
