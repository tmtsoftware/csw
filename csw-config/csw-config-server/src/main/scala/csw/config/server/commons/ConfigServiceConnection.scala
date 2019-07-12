package csw.config.server.commons

import csw.location.model.scaladsl.Connection.HttpConnection
import csw.location.model.scaladsl.{ComponentId, ComponentType}

/**
 * `ConfigServiceConnection` is a wrapper over predefined `HttpConnection` representing config server. It is used to register
 * with location service.
 */
object ConfigServiceConnection {
  val value = HttpConnection(ComponentId("ConfigServer", ComponentType.Service))
}
