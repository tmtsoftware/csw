package csw.auth

import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Directives._

object SecurityDirectives {

  type AuthorizationPolicy = AccessToken => Boolean

  def permission(name: String): Directive0 =
    authenticateOAuth2("master", Authentication.authenticator).flatMap { at =>
      authorize(Authorization.hasPermission(at, name))
    }

  def role(name: String): Directive0 =
    authenticateOAuth2("master", Authentication.authenticator).flatMap { at =>
      authorize(Authorization.hasRole(at, name))
    }

  def customPolicy(policy: AuthorizationPolicy): Directive0 = {
    authenticateOAuth2("master", Authentication.authenticator).flatMap { at =>
      authorize(policy(at))
    }
  }
}
