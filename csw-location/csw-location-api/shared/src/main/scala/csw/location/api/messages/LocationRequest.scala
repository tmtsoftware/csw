/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.api.messages

import csw.location.api.models.*

import scala.concurrent.duration.FiniteDuration

sealed trait LocationRequest

object LocationRequest {
  case class Register(registration: Registration)                                   extends LocationRequest
  case class Unregister(connection: Connection)                                     extends LocationRequest
  case object UnregisterAll                                                         extends LocationRequest
  case class Find(connection: TypedConnection[Location])                            extends LocationRequest
  case class Resolve(connection: TypedConnection[Location], within: FiniteDuration) extends LocationRequest
  case object ListEntries                                                           extends LocationRequest
  case class ListByComponentType(componentType: ComponentType)                      extends LocationRequest
  case class ListByHostname(hostname: String)                                       extends LocationRequest
  case class ListByConnectionType(connectionType: ConnectionType)                   extends LocationRequest
  case class ListByPrefix(prefix: String)                                           extends LocationRequest
}
