package csw.auth.adapters.akka.http

import akka.http.scaladsl.server.Directives.{authenticateOAuth2, authorize}
import akka.http.scaladsl.server.{Directive, Directive0, Directive1, Directives}
import csw.auth.core.Keycloak
import csw.auth.core.token.AccessToken

class SecurityDirectives(authentication: Authentication) {

  private val realm: String = Keycloak.deployment.getRealm

  def permission(name: String, resource: String = "Default Resource"): Directive0 =
    authenticateOAuth2(realm, authentication.authenticator).flatMap { at =>
      authorize(at.hasPermission(name, resource))
    }

  def realmRole(name: String): Directive0 =
    authenticateOAuth2(realm, authentication.authenticator).flatMap { at =>
      authorize(at.hasRealmRole(name))
    }

  def resourceRole(name: String): Directive0 =
    authenticateOAuth2(realm, authentication.authenticator).flatMap { at =>
      authorize(at.hasResourceRole(name))
    }

  def customPolicy(policy: AccessToken => Boolean): Directive0 = {
    authenticateOAuth2(realm, authentication.authenticator).flatMap { at =>
      authorize(policy(at))
    }
  }

  def customPolicy(policy: => Boolean): Directive0 = {
    authenticateOAuth2(realm, authentication.authenticator).flatMap { _ =>
      authorize(policy)
    }
  }

  def withUser: Directive1[User] = {
    authenticateOAuth2(realm, authentication.authenticator)
      .map(at => User(at))
      .flatMap {
        case Some(user) => Directives.provide(user)
        case _          => akka.http.scaladsl.server.Directives.reject(akka.http.javadsl.server.Rejections.authorizationFailed)
      }
  }
}

object SecurityDirectives {
  def apply(authentication: Authentication): SecurityDirectives = new SecurityDirectives(authentication)
}
