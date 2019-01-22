package csw.aas.http

import csw.aas.core.token.AccessToken

/**
 * An authorization policy is a way to provide filter incoming HTTP requests based on standard rules.
 */
sealed trait AuthorizationPolicy

/**
 * An authorization policy is a way to provide filter incoming HTTP requests based on standard rules.
 */
object AuthorizationPolicy {

  /**
   * This policy filters requests based on Resource Role. A Resource Role is a client specific role.
   * @param name Name of role
   */
  final case class ResourceRolePolicy(name: String) extends AuthorizationPolicy

  /**
   * This policy filters requests based on Realm Role.
   *
   * A Realm Role is global role within a realm and is applicable for all clients within realm.
   * @param name Name of role
   */
  final case class RealmRolePolicy(name: String) extends AuthorizationPolicy

  /**
   * This policy filters requests based on permissions.
   * @param scope Name of scope
   * @param resource Name of resource for which permissions is applicable.
   */
  final case class PermissionPolicy(scope: String, resource: String = "Default Resource") extends AuthorizationPolicy

  /**
   * Allows custom request filtering based on access token properties.
   * @param predicate Filter
   */
  final case class CustomPolicy(predicate: AccessToken => Boolean) extends AuthorizationPolicy

  /**
   * Use this when you only need authentication but not authorization
   */
  case object EmptyPolicy extends AuthorizationPolicy
}
