package csw.config.server.commons

import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.HttpRegistration

object RegistrationFactory {
  def http(connection: HttpConnection, port: Int, path: String) = HttpRegistration(connection, port, path, null)
}
