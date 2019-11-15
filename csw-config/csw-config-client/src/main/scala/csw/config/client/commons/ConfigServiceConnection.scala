package csw.config.client.commons

import csw.location.models.ComponentId
import csw.location.models.Connection.HttpConnection
import csw.location.models.ComponentType
import csw.params.core.models.{Prefix, Subsystem}

/**
 * `ConfigServiceConnection` is a wrapper over predefined `HttpConnection` representing config server. It is used to resolve
 * config service location for client in `ConfigServiceResolver`
 */
private[csw] object ConfigServiceConnection {
  val value = HttpConnection(ComponentId(Prefix(Subsystem.CSW, "ConfigServer"), ComponentType.Service))
}
