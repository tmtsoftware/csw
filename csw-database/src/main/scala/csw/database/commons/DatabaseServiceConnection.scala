package csw.database.commons

import csw.location.model.scaladsl.Connection.TcpConnection
import csw.location.model.scaladsl.{ComponentId, ComponentType}

/**
 * `DatabaseServiceConnection` is a wrapper over predefined `TcpConnection` representing database service. It is used to resolve
 * database service location.
 */
private[database] object DatabaseServiceConnection {
  val value = TcpConnection(ComponentId("DatabaseServer", ComponentType.Service))
}
