package csw.services.config.client.commons

import java.net.URI

import csw.services.location.api.models.Connection.HttpConnection
import csw.services.location.api.models.HttpLocation

object LocationFactory {
  def http(connection: HttpConnection, uri: URI) = HttpLocation(connection, uri, null)
}
