package csw.services.commons

import csw.messages.location.Connection.HttpConnection
import csw.services.location.models.HttpRegistration

object RegistrationFactory {
  def http(connection: HttpConnection, port: Int, path: String) = HttpRegistration(connection, port, path, null)
}
