package csw.services.alarm.client.internal.commons

import csw.messages.location.Connection.TcpConnection
import csw.messages.location.{ComponentId, ComponentType}

/**
 * `AlarmServiceConnection` is a wrapper over predefined `TcpConnection` representing alarm service. It is used to resolve
 * alarm service location.
 */
object AlarmServiceConnection {
  val value = TcpConnection(ComponentId("AlarmServer", ComponentType.Service))
}
