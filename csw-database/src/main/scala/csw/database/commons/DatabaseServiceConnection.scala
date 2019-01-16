package csw.database.commons

import csw.location.api.models.Connection.TcpConnection
import csw.location.api.models.{ComponentId, ComponentType}

/**
 * `DatabaseServiceConnection` is a wrapper over predefined `TcpConnection` representing alarm service. It is used to resolve
 * database service location.
 */
private[database] object DatabaseServiceConnection {
  val value = TcpConnection(ComponentId("DatabaseServer", ComponentType.Service))
}
