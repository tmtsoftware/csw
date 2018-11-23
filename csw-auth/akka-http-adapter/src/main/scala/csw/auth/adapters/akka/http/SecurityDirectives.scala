package csw.auth.adapters.akka.http

import akka.http.scaladsl.server.Directives.{authenticateOAuth2, authorize}
import akka.http.scaladsl.server._
import csw.auth.core.Keycloak
import csw.auth.core.token.AccessToken

class SecurityDirectives(authentication: Authentication) {

  private val realm: String = Keycloak.deployment.getRealm

  def permission(name: String, resource: String = "Default Resource"): Directive0 =
    authenticate.flatMap { at =>
      authorize(at.hasPermission(name, resource))
    }

  def realmRole(name: String): Directive0 =
    authenticate.flatMap { at =>
      authorize(at.hasRealmRole(name))
    }

  def resourceRole(name: String): Directive0 =
    authenticate.flatMap { at =>
      authorize(at.hasResourceRole(name))
    }

  def customPolicy(policy: AccessToken => Boolean): Directive0 = {
    authenticate.flatMap { at =>
      authorize(policy(at))
    }
  }

  def customPolicy(policy: => Boolean): Directive0 = {
    authenticate.flatMap { _ =>
      authorize(policy)
    }
  }

  def permissionWithUser(name: String, resource: String = "Default Resource"): Directive1[User] =
    authenticate.flatMap { at =>
      authorize(at.hasPermission(name, resource)) & user(at)
    }

  def realmRoleWithUser(name: String): Directive1[User] =
    authenticate.flatMap { at =>
      authorize(at.hasRealmRole(name)) & user(at)
    }

  def resourceRoleWithUser(name: String): Directive1[User] =
    authenticate.flatMap { at =>
      authorize(at.hasResourceRole(name)) & user(at)
    }

  def customPolicyWithUser(policy: AccessToken => Boolean): Directive1[User] = {
    authenticate.flatMap { at =>
      authorize(policy(at)) & user(at)
    }
  }

  def customPolicyWithUser(policy: => Boolean): Directive1[User] = {
    authenticate.flatMap { at =>
      authorize(policy) & user(at)
    }
  }

  private def authenticate: Directive1[AccessToken] = {
    authenticateOAuth2(realm, authentication.authenticator)
  }

  private def user(at: AccessToken): Directive1[User] = {
    User(at) match {
      case Some(user) => Directives.provide(user)
      case _          => akka.http.scaladsl.server.Directives.reject(akka.http.javadsl.server.Rejections.authorizationFailed)
    }
  }
}

object SecurityDirectives {
  def apply(authentication: Authentication): SecurityDirectives = new SecurityDirectives(authentication)
}
