package csw.auth.adapters.akka.http

import akka.http.scaladsl.model.{HttpMethod, HttpMethods}
import akka.http.scaladsl.server.Directives.{authenticateOAuth2, authorize => keycloakAuthorize, _}
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.AuthenticationDirective
import csw.auth.adapters.akka.http.AuthorizationPolicy._
import csw.auth.core.Keycloak
import csw.auth.core.token.AccessToken

class SecurityDirectives(authentication: Authentication) {

  private val realm: String = Keycloak.deployment.getRealm

  private[auth] def authenticate: AuthenticationDirective[AccessToken] = authenticateOAuth2(realm, authentication.authenticator)

  private[auth] def authorize(authorizationPolicy: AuthorizationPolicy, accessToken: AccessToken): Directive0 =
    authorizationPolicy match {
      case ResourceRolePolicy(name)         => keycloakAuthorize(accessToken.hasResourceRole(name))
      case RealmRolePolicy(name)            => keycloakAuthorize(accessToken.hasRealmRole(name))
      case PermissionPolicy(name, resource) => keycloakAuthorize(accessToken.hasPermission(name, resource))
      case CustomPolicy(predicate)          => keycloakAuthorize(predicate(accessToken))
      case EmptyPolicy                      => Directive.Empty
    }

  private def sMethod(httpMethod: HttpMethod, authorizationPolicy: AuthorizationPolicy): Directive1[AccessToken] =
    method(httpMethod) & authenticate.flatMap(token => authorize(authorizationPolicy, token) & provide(token))

  def sPost(authorizationPolicy: AuthorizationPolicy): Directive1[AccessToken] = sMethod(HttpMethods.POST, authorizationPolicy)

  def sGet(authorizationPolicy: AuthorizationPolicy): Directive1[AccessToken] = sMethod(HttpMethods.GET, authorizationPolicy)

  def sPut(authorizationPolicy: AuthorizationPolicy): Directive1[AccessToken] = sMethod(HttpMethods.PUT, authorizationPolicy)

  def sDelete(authorizationPolicy: AuthorizationPolicy): Directive1[AccessToken] =
    sMethod(HttpMethods.DELETE, authorizationPolicy)

  def sPatch(authorizationPolicy: AuthorizationPolicy): Directive1[AccessToken] = sMethod(HttpMethods.PATCH, authorizationPolicy)

  def sHead(authorizationPolicy: AuthorizationPolicy): Directive1[AccessToken] = sMethod(HttpMethods.HEAD, authorizationPolicy)

  def sConnect(authorizationPolicy: AuthorizationPolicy): Directive1[AccessToken] =
    sMethod(HttpMethods.CONNECT, authorizationPolicy)
}

object SecurityDirectives {
  def apply(authentication: Authentication): SecurityDirectives = new SecurityDirectives(authentication)
}
