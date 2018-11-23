package csw.auth.adapters.akka.http

import akka.http.javadsl.server.Rejections
import akka.http.scaladsl.server.Directives.{authenticateOAuth2, authorize}
import akka.http.scaladsl.server.directives.AuthenticationDirective
import akka.http.scaladsl.server.{Route, StandardRoute, _}
import csw.auth.core.Keycloak
import csw.auth.core.token.AccessToken

class SecurityDirectives(authentication: Authentication) {

  private val realm: String = Keycloak.deployment.getRealm

  def permission(name: String, resource: String = "Default Resource")(f: Route)(implicit at: AccessToken): Route =
    authorize(at.hasPermission(name, resource)) & StandardRoute(f)

  def realmRole(name: String)(f: Route)(implicit at: AccessToken): Route =
    authorize(at.hasRealmRole(name)) & StandardRoute(f)

  def resourceRole(name: String)(f: Route)(implicit at: AccessToken): Route =
    authorize(at.hasResourceRole(name)) & StandardRoute(f)

  def customPolicy(policy: AccessToken => Boolean)(f: Route)(implicit at: AccessToken): Route = {
    authorize(policy(at)) & StandardRoute(f)
  }

  def customPolicy(policy: => Boolean)(f: Route): Route = {
    authorize(policy) & StandardRoute(f)
  }

  def secure: AuthenticationDirective[AccessToken] = {
    authenticateOAuth2(realm, authentication.authenticator)
  }

  def user(f: User ⇒ Route)(implicit accessToken: AccessToken): Route = {
    User(accessToken) match {
      case Some(user) ⇒ f(user)
      case None       ⇒ Directives.reject(Rejections.authorizationFailed)
    }
  }
}

object SecurityDirectives {
  def apply(authentication: Authentication): SecurityDirectives = new SecurityDirectives(authentication)
}
