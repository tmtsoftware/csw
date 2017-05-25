package csw.services.config.server.commons

import csw.services.location.models.{ComponentId, ComponentType}
import csw.services.location.models.Connection.HttpConnection

object ConfigServiceConnection extends HttpConnection(ComponentId("config", ComponentType.Service))
