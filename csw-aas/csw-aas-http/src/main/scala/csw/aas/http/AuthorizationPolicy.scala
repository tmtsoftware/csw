package csw.aas.http

import csw.aas.core.token.AccessToken
import csw.aas.http.AuthorizationPolicy.{AndPolicy, OrPolicy}

import scala.concurrent.Future

/**
 * An authorization policy is a way to filter incoming HTTP requests based on rules
 */
sealed trait AuthorizationPolicy {

  /**
   * Applies a new authorization policy in combination with previous policy.
   * Passing of both policies is requried for authorization to succeed.
   *
   * @param authorizationPolicy new Authorization policy
   * @return combined authorization policy
   */
  def &(authorizationPolicy: AuthorizationPolicy): AuthorizationPolicy = AndPolicy(this, authorizationPolicy)

  /**
   * Applies a new authorization policy if the previous policy fails.
   * Authorization will succeed if any of the provided policy passes.
   *
   * @param authorizationPolicy new Authorization policy
   * @return combined authorization policy
   */
  def |(authorizationPolicy: AuthorizationPolicy): AuthorizationPolicy = OrPolicy(this, authorizationPolicy)
}

/**
 * An authorization policy is a way to provide filter incoming HTTP requests based on standard rules.
 */
object AuthorizationPolicy {

  /**
   * This policy filters requests based on client specific roles
   *
   * @param name Name of role
   */
  final case class ClientRolePolicy(name: String) extends AuthorizationPolicy

  /**
   * This policy filters requests based on Realm Role.
   *
   * A Realm Role is global role within a realm and is applicable for all clients within realm.
   *
   * @param name Name of role
   */
  final case class RealmRolePolicy(name: String) extends AuthorizationPolicy

  /**
   * Allows custom request filtering based on access token properties.
   *
   * @param predicate Filter
   */
  final case class CustomPolicy(predicate: AccessToken => Boolean) extends AuthorizationPolicy

  /**
   * Allows custom request filtering based on access token properties.
   *
   * @param predicate Async filter
   */
  final case class CustomPolicyAsync(predicate: AccessToken => Future[Boolean]) extends AuthorizationPolicy

  /**
   * Use this when you only need authentication but not authorization
   */
  case object EmptyPolicy extends AuthorizationPolicy

  final case class AndPolicy(left: AuthorizationPolicy, right: AuthorizationPolicy) extends AuthorizationPolicy
  final case class OrPolicy(left: AuthorizationPolicy, right: AuthorizationPolicy)  extends AuthorizationPolicy
}
