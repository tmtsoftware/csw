package csw.aas.http

import csw.aas.core.token.AccessToken

sealed trait AuthorizationPolicy

object AuthorizationPolicy {
  case class ResourceRolePolicy(name: String)                                      extends AuthorizationPolicy
  case class RealmRolePolicy(name: String)                                         extends AuthorizationPolicy
  case class PermissionPolicy(name: String, resource: String = "Default Resource") extends AuthorizationPolicy
  case class CustomPolicy(predicate: AccessToken => Boolean)                       extends AuthorizationPolicy
  case object EmptyPolicy                                                          extends AuthorizationPolicy
}
