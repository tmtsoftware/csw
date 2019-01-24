package csw.aas.http

import csw.aas.core.token.AccessToken
import csw.aas.http.AuthorizationPolicy.PolicyExpression
import csw.aas.http.AuthorizationPolicy.PolicyExpression.{And, ExpressionOperator, Or}

import scala.concurrent.Future

/**
 * An authorization policy is a way to filter incoming HTTP requests based on rules
 */
sealed trait AuthorizationPolicy {

  /**
   * Applies a new authorization policy in combination with previous policy.
   * Passing of both policies is requried for authorization to succeed.
   * @param authorizationPolicy new Authorization policy
   * @return combined authorization policy
   */
  def &(authorizationPolicy: AuthorizationPolicy): AuthorizationPolicy = {
    PolicyExpression(this, And, authorizationPolicy)
  }

  /**
   * Applies a new authorization policy if the previous policy fails.
   * Authorization will succeed if any of the provided policy passes.
   * @param authorizationPolicy new Authorization policy
   * @return combined authorization policy
   */
  def |(authorizationPolicy: AuthorizationPolicy): AuthorizationPolicy = {
    PolicyExpression(this, Or, authorizationPolicy)
  }
}

/**
 * An authorization policy is a way to provide filter incoming HTTP requests based on standard rules.
 */
object AuthorizationPolicy {

  private[aas] final case class PolicyExpression(left: AuthorizationPolicy,
                                                 operator: ExpressionOperator,
                                                 right: AuthorizationPolicy)
      extends AuthorizationPolicy

  private[aas] object PolicyExpression {
    trait ExpressionOperator
    case object Or extends ExpressionOperator {
      override def toString: String = "|"
    }
    case object And extends ExpressionOperator {
      override def toString: String = "&"
    }
  }

  /**
   * This policy filters requests based on client specific roles
   * @param name Name of role
   */
  final case class ClientRolePolicy(name: String) extends AuthorizationPolicy

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
   * Allows custom request filtering based on access token properties.
   * @param predicate Async filter
   */
  final case class CustomPolicyAsync(predicate: AccessToken => Future[Boolean]) extends AuthorizationPolicy

  /**
   * Use this when you only need authentication but not authorization
   */
  case object EmptyPolicy extends AuthorizationPolicy
}
