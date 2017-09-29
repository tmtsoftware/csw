package csw.services.config.server.commons

import csw.param.models.location.Connection.HttpConnection
import csw.param.models.location.{ComponentId, ComponentType}

object ConfigServiceConnection {
  val value = HttpConnection(ComponentId("config", ComponentType.Service))
}
