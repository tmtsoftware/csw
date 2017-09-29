package csw.services.config.client.commons

import csw.messages.models.location.Connection.HttpConnection
import csw.messages.models.location.{ComponentId, ComponentType}

object ConfigServiceConnection {
  val value = HttpConnection(ComponentId("config", ComponentType.Service))
}
