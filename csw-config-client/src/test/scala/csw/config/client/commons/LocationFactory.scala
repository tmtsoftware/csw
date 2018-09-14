package csw.config.client.commons

import java.net.URI

import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.HttpLocation

object LocationFactory {
  def http(connection: HttpConnection, uri: URI) = HttpLocation(connection, uri, null)
}
