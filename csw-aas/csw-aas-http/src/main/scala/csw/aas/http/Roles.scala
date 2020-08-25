package csw.aas.http

import csw.aas.core.token.AccessToken
import csw.prefix.models.Subsystem

case class Roles private (roles: Set[String]) {
  def intersect(that: Roles): Set[String]             = this.roles.intersect(that.roles)
  def containsUserRole(subsystem: Subsystem): Boolean = roles.contains(subsystem.name.toLowerCase + "-user")
  def containsAnyRole(subsystem: Subsystem): Boolean  = roles.exists(_.contains(subsystem.name.toLowerCase))
}
object Roles {
  def apply(roles: Set[String]): Roles = new Roles(roles.map(_.toLowerCase))
  def apply(token: AccessToken): Roles = Roles(token.realm_access.roles)
}
