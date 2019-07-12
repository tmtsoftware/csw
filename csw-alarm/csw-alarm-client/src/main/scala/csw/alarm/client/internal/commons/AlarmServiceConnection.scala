package csw.alarm.client.internal.commons

import csw.location.model.scaladsl.Connection.TcpConnection
import csw.location.model.scaladsl.{ComponentId, ComponentType}

/**
 * `AlarmServiceConnection` is a wrapper over predefined `TcpConnection` representing alarm service. It is used to resolve
 * alarm service location.
 */
private[csw] object AlarmServiceConnection {
  val value = TcpConnection(ComponentId("AlarmServer", ComponentType.Service))
}
