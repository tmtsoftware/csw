package csw.database.commons

import csw.location.models.Connection.TcpConnection
import csw.location.models.{ComponentId, ComponentType}
import csw.params.core.models.{Prefix, Subsystem}

/**
 * `DatabaseServiceConnection` is a wrapper over predefined `TcpConnection` representing database service. It is used to resolve
 * database service location.
 */
private[database] object DatabaseServiceConnection {
  val value = TcpConnection(ComponentId(Prefix(Subsystem.CSW, "DatabaseServer"), ComponentType.Service))
}
