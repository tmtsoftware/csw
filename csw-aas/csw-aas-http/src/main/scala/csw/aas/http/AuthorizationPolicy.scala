package csw.aas.http

import csw.aas.core.token.AccessToken

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
 * An asynchronous authorization policy is a way to filter incoming HTTP requests based on rules
 */
trait AuthorizationPolicy {

  /**
   * Implement this method to create an asynchronous AuthorizationPolicy
   */
  def authorize(accessToken: AccessToken): Future[Boolean]

  /**
   * Applies a new authorization policy in combination with previous policy.
   * Passing of both policies is requried for authorization to succeed.
   *
   * @param other new Authorization policy
   * @return combined authorization policy
   */
  def &(other: AuthorizationPolicy)(implicit ec: ExecutionContext): AuthorizationPolicy = { accessToken =>
    val leftF  = authorize(accessToken)
    val rightF = other.authorize(accessToken)
    leftF.zipWith(rightF)(_ && _)
  }

  /**
   * Applies a new authorization policy if the previous policy fails.
   *
   * Authorization will succeed if any of the provided policy passes.
   *
   * @param other new Authorization policy
   * @return combined authorization policy
   */
  def |(other: AuthorizationPolicy)(implicit ec: ExecutionContext): AuthorizationPolicy = { accessToken =>
    val leftF  = authorize(accessToken)
    val rightF = other.authorize(accessToken)
    leftF.zipWith(rightF)(_ || _)
  }

}

/**
 * A synchronous authorization policy is a way to filter incoming HTTP requests based on rules
 */
trait SyncAuthorizationPolicy extends AuthorizationPolicy {

  /**
   * Implement this method to create a synchronous AuthorizationPolicy
   */
  protected def syncAuthorize(accessToken: AccessToken): Boolean

  final override def authorize(accessToken: AccessToken): Future[Boolean] = Future.fromTry(Try(syncAuthorize(accessToken)))
}

/**
 * An authorization policy is a way to provide filter incoming HTTP requests based on standard rules.
 */
object AuthorizationPolicy {

  /**
   * This policy filters requests based on Realm Role.
   *
   * A Realm Role is global role within a realm and is applicable for all clients within realm.
   *
   * @param name Name of role
   */
  final case class RealmRolePolicy(name: String) extends SyncAuthorizationPolicy {
    override protected def syncAuthorize(accessToken: AccessToken): Boolean = accessToken.hasRealmRole(name)
  }

  /**
   * Allows custom request filtering based on access token properties.
   *
   * @param predicate Filter
   */
  final case class CustomPolicy(predicate: AccessToken => Boolean) extends SyncAuthorizationPolicy {
    override protected def syncAuthorize(accessToken: AccessToken): Boolean = predicate(accessToken)
  }

  /**
   * Allows custom request filtering based on access token properties.
   *
   * @param predicate Async filter
   */
  final case class CustomPolicyAsync(predicate: AccessToken => Future[Boolean]) extends AuthorizationPolicy {
    override def authorize(accessToken: AccessToken): Future[Boolean] = predicate(accessToken)
  }

  /**
   * Use this when you only need authentication but not authorization
   */
  case object EmptyPolicy extends SyncAuthorizationPolicy {
    override protected def syncAuthorize(accessToken: AccessToken): Boolean = true
  }
}
