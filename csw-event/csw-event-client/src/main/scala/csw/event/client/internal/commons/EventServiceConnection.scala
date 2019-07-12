package csw.event.client.internal.commons

import csw.location.model.scaladsl.Connection.TcpConnection
import csw.location.model.scaladsl.{ComponentId, ComponentType}

/**
 * `EventServiceConnection` is a wrapper over predefined `TcpConnection` representing event service. It is used to resolve
 * event service location for client in `EventServiceResolver`
 */
private[csw] object EventServiceConnection {
  val value = TcpConnection(ComponentId("EventServer", ComponentType.Service))
}
