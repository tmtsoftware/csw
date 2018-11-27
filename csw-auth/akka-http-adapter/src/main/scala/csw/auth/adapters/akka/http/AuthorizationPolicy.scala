package csw.auth.adapters.akka.http
import csw.auth.core.token.AccessToken

sealed trait AuthorizationPolicy

sealed trait PermissionPolicy extends AuthorizationPolicy {
  val name: String
  val resource: String
}
sealed trait RolePolicy extends AuthorizationPolicy {
  val name: String
}

object AuthorizationPolicy {
  case class ResourceRolePolicy(name: String)                                   extends RolePolicy
  case class RealmRolePolicy(name: String)                                      extends RolePolicy
  case class PermissionPolicyWithCustomResource(name: String, resource: String) extends PermissionPolicy
  case class PermissionPolicyWithDefaultResource(name: String) extends PermissionPolicy {
    val resource: String = "Default Resource"
  }
  case class CustomPolicy(predicate: AccessToken => Boolean) extends AuthorizationPolicy
}
