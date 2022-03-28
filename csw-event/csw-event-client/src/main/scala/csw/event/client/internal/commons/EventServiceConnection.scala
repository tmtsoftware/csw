/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.event.client.internal.commons

import csw.location.api.models.{ComponentId, ComponentType}
import csw.location.api.models.Connection.TcpConnection
import csw.prefix.models.{Prefix, Subsystem}

/**
 * `EventServiceConnection` is a wrapper over predefined `TcpConnection` representing event service. It is used to resolve
 * event service location for client in `EventServiceResolver`
 */
private[csw] object EventServiceConnection {
  val value = TcpConnection(ComponentId(Prefix(Subsystem.CSW, "EventServer"), ComponentType.Service))
}
