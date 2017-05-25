package csw.services.config.server.commons

import csw.services.location.models.Connection.HttpConnection
import csw.services.location.models.{ComponentId, ComponentType}

object ConfigServiceConnection {
  val value = HttpConnection(ComponentId("config", ComponentType.Service))
}
