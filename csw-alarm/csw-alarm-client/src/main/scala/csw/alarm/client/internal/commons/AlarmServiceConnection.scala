package csw.alarm.client.internal.commons

import csw.location.models.ComponentId
import csw.location.models.Connection.TcpConnection
import csw.location.models.ComponentType
import csw.params.core.models.{Prefix, Subsystem}

/**
 * `AlarmServiceConnection` is a wrapper over predefined `TcpConnection` representing alarm service. It is used to resolve
 * alarm service location.
 */
private[csw] object AlarmServiceConnection {
  val value = TcpConnection(ComponentId(Prefix(Subsystem.CSW, "AlarmServer"), ComponentType.Service))
}
