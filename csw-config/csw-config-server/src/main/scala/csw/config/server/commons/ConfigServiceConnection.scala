package csw.config.server.commons

import csw.location.models.{ComponentId, ComponentType}
import csw.location.models.Connection.HttpConnection
import csw.prefix.models.Subsystem
import csw.prefix.models.Prefix

/**
 * `ConfigServiceConnection` is a wrapper over predefined `HttpConnection` representing config server. It is used to register
 * with location service.
 */
object ConfigServiceConnection {
  val value = HttpConnection(ComponentId(Prefix(Subsystem.CSW, "ConfigServer"), ComponentType.Service))
}
