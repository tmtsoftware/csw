package csw.services.integtration.common

import csw.messages.location.Connection.HttpConnection
import csw.messages.location.models.HttpRegistration

object RegistrationFactory {
  def http(connection: HttpConnection, port: Int, path: String) = HttpRegistration(connection, port, path, null)
}
