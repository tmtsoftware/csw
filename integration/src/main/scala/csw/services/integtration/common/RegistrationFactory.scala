package csw.services.integtration.common

import csw.messages.location.Connection.HttpConnection
import csw.services.location.api.models.HttpRegistration

object RegistrationFactory {
  def http(connection: HttpConnection, port: Int, path: String) = HttpRegistration(connection, port, path, null)
}
