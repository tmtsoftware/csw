package csw.services.config.server.commons

import csw.services.location.api.models.Connection.HttpConnection
import csw.services.location.api.models.{ComponentId, ComponentType}

/**
 * `ConfigServiceConnection` is a wrapper over predefined `HttpConnection` representing config server. It is used to register
 * with location service.
 */
object ConfigServiceConnection {
  val value = HttpConnection(ComponentId("ConfigServer", ComponentType.Service))
}
