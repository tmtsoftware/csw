package csw.auth.adapters.akka.http

import akka.http.scaladsl.model.{HttpMethod, HttpMethods}
import akka.http.scaladsl.server.Directives.{authenticateOAuth2, authorize}
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.AuthenticationDirective
import csw.auth.core.Keycloak
import csw.auth.core.token.AccessToken
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.util.Tuple

class SecurityDirectives(authentication: Authentication) {

  private val realm: String = Keycloak.deployment.getRealm

  private[auth] def permission(name: String, at: AccessToken, resource: String = "Default Resource"): Directive0 =
    authorize(at.hasPermission(name, resource))

  private[auth] def realmRole(name: String, at: AccessToken): Directive0 = authorize(at.hasRealmRole(name))

  private[auth] def resourceRole(name: String, at: AccessToken): Directive0 = authorize(at.hasResourceRole(name))

  private[auth] def customPolicy(policy: => Boolean): Directive0 = authorize(policy)

  private[auth] def secure: AuthenticationDirective[AccessToken] = authenticateOAuth2(realm, authentication.authenticator)

  private val sMethod
    : HttpMethod => (Option[String], Option[String], Option[String], Option[String], Option[() => Boolean]) => Directive1[
      AccessToken
    ] = { httpMethod =>
    {
      case (Some(resourceRole1), None, None, None, None) =>
        method(httpMethod) & secure.flatMap(at => {
          resourceRole(resourceRole1, at) & provide(at)
        })
      case (None, Some(realmRole1), None, None, None) =>
        method(httpMethod) & secure.flatMap(at => {
          realmRole(realmRole1, at) & provide(at)
        })
      case (None, None, Some(scope1), None, None) =>
        method(httpMethod) & secure.flatMap(at => {
          permission(scope1, at) & provide(at)
        })
      case (None, None, Some(scope1), Some(resource1), None) =>
        method(httpMethod) & secure.flatMap(at => {
          permission(scope1, at, resource1) & provide(at)
        })
      case (None, None, None, None, Some(f)) =>
        method(httpMethod) & secure.flatMap(at => {
          customPolicy(f()) & provide(at)
        })
      case _ => throw new RuntimeException("invalid combination of parameters")
    }
  }

  //noinspection ScalaStyle
  def sPost(resourceRole: String = null,
            realmRole: String = null,
            permission: String = null,
            resource: String = null,
            customPolicy: () => Boolean = null): Directive1[AccessToken] = {
    sMethod(HttpMethods.POST)(Option(resourceRole), Option(realmRole), Option(permission), Option(resource), Option(customPolicy))
  }

  //noinspection ScalaStyle
  def sGet(resourceRole: String = null,
           realmRole: String = null,
           permission: String = null,
           resource: String = null,
           customPolicy: () => Boolean = null): Directive1[AccessToken] = {
    sMethod(HttpMethods.GET)(Option(resourceRole), Option(realmRole), Option(permission), Option(resource), Option(customPolicy))
  }

  //noinspection ScalaStyle
  def sPut(resourceRole: String = null,
           realmRole: String = null,
           permission: String = null,
           resource: String = null,
           customPolicy: () => Boolean = null): Directive1[AccessToken] = {
    sMethod(HttpMethods.PUT)(Option(resourceRole), Option(realmRole), Option(permission), Option(resource), Option(customPolicy))
  }

  //noinspection ScalaStyle
  def sDelete(resourceRole: String = null,
              realmRole: String = null,
              permission: String = null,
              resource: String = null,
              customPolicy: () => Boolean = null): Directive1[AccessToken] = {
    sMethod(HttpMethods.DELETE)(Option(resourceRole),
                                Option(realmRole),
                                Option(permission),
                                Option(resource),
                                Option(customPolicy))
  }
}

object SecurityDirectives {
  def apply(authentication: Authentication): SecurityDirectives = new SecurityDirectives(authentication)
}
