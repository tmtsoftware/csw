package csw.services.event.internal.commons

import csw.messages.location.Connection.TcpConnection
import csw.messages.location.{ComponentId, ComponentType}

/**
 * `EventServiceConnection` is a wrapper over predefined `TcpConnection` representing event service. It is used to resolve
 * event service location for client in `EventServiceResolver`
 */
private[csw] object EventServiceConnection {
  val value = TcpConnection(ComponentId("EventServer", ComponentType.Service))
}
