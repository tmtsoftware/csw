/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.aas.http

import csw.prefix.models.Subsystem
import msocket.security.models.AccessToken

case class Roles private (roles: Set[String]) {
  def intersect(that: Roles): Set[String]             = this.roles.intersect(that.roles)
  def containsUserRole(subsystem: Subsystem): Boolean = roles.contains(subsystem.name.toLowerCase + "-user")
  def containsEngRole(subsystem: Subsystem): Boolean  = roles.contains(subsystem.name.toLowerCase + "-eng")
  def containsAnyRole(subsystem: Subsystem): Boolean  = roles.exists(_.contains(subsystem.name.toLowerCase))
}
object Roles {
  def apply(roles: Set[String]): Roles = new Roles(roles.map(_.toLowerCase))
  def apply(token: AccessToken): Roles = Roles(token.realm_access.roles)
}
