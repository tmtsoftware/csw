package csw.services.config.server.commons

import csw.messages.location.Connection.HttpConnection
import csw.messages.location.{ComponentId, ComponentType}

/**
 * `ConfigServiceConnection` is a wrapper over predefined `HttpConnection` representing config server. It is used to register
 * with location service.
 */
private[config] object ConfigServiceConnection {
  val value = HttpConnection(ComponentId("ConfigServer", ComponentType.Service))
}
