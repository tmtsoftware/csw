/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.alarm.client.internal.commons

import csw.location.api.models.{ComponentId, ComponentType}
import csw.location.api.models.Connection.TcpConnection
import csw.prefix.models.{Prefix, Subsystem}

/**
 * `AlarmServiceConnection` is a wrapper over predefined `TcpConnection` representing alarm service. It is used to resolve
 * alarm service location.
 */
private[csw] object AlarmServiceConnection {
  val value = TcpConnection(ComponentId(Prefix(Subsystem.CSW, "AlarmServer"), ComponentType.Service))
}
