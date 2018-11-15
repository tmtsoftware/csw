package csw.auth

import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Directives._

object SecurityDirectives {

  type AuthorizationPolicy = AccessToken => Boolean

  def permission(name: String, resource: String): Directive0 =
    authenticateOAuth2("master", Authentication.authenticator).flatMap { at =>
      authorize(at.hasPermission(name, resource))
    }

  def role(name: String, resource: String): Directive0 =
    authenticateOAuth2("master", Authentication.authenticator).flatMap { at =>
      authorize(at.hasRole(name, resource))
    }

  def customPolicy(policy: AuthorizationPolicy): Directive0 = {
    authenticateOAuth2("master", Authentication.authenticator).flatMap { at =>
      authorize(policy(at))
    }
  }

  def customPolicy(predicate: => Boolean): Directive0 = {
    authenticateOAuth2("master", Authentication.authenticator).flatMap { at =>
      authorize(predicate)
    }
  }
}
