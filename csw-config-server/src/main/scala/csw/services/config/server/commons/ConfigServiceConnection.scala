package csw.services.config.server.commons

import csw.messages.location.Connection.HttpConnection
import csw.messages.location.{ComponentId, ComponentType}

object ConfigServiceConnection {
  val value = HttpConnection(ComponentId("ConfigServer", ComponentType.Service))
}
