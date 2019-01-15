package csw.alarm.client.internal.commons

import csw.location.api.models.Connection.TcpConnection
import csw.location.api.models.{ComponentId, ComponentType}

/**
 * `AlarmServiceConnection` is a wrapper over predefined `TcpConnection` representing alarm service. It is used to resolve
 * alarm service location.
 */
object AlarmServiceConnection {
  val value = TcpConnection(ComponentId("AlarmServer", ComponentType.Service))
}
