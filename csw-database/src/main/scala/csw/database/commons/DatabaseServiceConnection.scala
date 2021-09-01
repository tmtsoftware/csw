package csw.database.commons

import csw.location.api.models.{ComponentId, ComponentType}
import csw.location.api.models.Connection.TcpConnection
import csw.prefix.models.{Prefix, Subsystem}

/**
 * `DatabaseServiceConnection` is a wrapper over predefined `TcpConnection` representing database service. It is used to resolve
 * database service location.
 */
private[csw] object DatabaseServiceConnection {
  val value = TcpConnection(ComponentId(Prefix(Subsystem.CSW, "DatabaseServer"), ComponentType.Service))
}
