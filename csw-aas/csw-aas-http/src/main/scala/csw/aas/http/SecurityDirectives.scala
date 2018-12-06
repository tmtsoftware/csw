package csw.aas.http

import akka.http.scaladsl.model.{HttpMethod, HttpMethods}
import akka.http.scaladsl.server.Directives.{authorize => keycloakAuthorize, _}
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.AuthenticationDirective
import csw.aas.core.commons.AuthLogger
import csw.aas.core.deployment.{AuthConfig, AuthServiceLocation}
import csw.aas.core.token.{AccessToken, TokenFactory}
import csw.aas.http.AuthorizationPolicy.{EmptyPolicy, _}
import csw.location.api.models.HttpLocation
import csw.location.api.scaladsl.LocationService
import org.keycloak.adapters.KeycloakDeployment

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, ExecutionContext}

class SecurityDirectives private[csw] (authentication: Authentication, realm: String, resourceName: String) {

  private val logger = AuthLogger.getLogger
  import logger._

  private[aas] def authenticate: AuthenticationDirective[AccessToken] =
    authenticateOAuth2Async(realm, authentication.authenticator)

  private[aas] def authorize(authorizationPolicy: AuthorizationPolicy, accessToken: AccessToken): Directive0 =
    authorizationPolicy match {
      case ResourceRolePolicy(name)         => keycloakAuthorize(accessToken.hasResourceRole(name, resourceName))
      case RealmRolePolicy(name)            => keycloakAuthorize(accessToken.hasRealmRole(name))
      case PermissionPolicy(name, resource) => keycloakAuthorize(accessToken.hasPermission(name, resource))
      case CustomPolicy(predicate) =>
        keycloakAuthorize({
          val result = predicate(accessToken)
          if (!result) debug(s"'${accessToken.userOrClientName}' failed custom policy authorization")
          else debug(s"authorization succeeded for '${accessToken.userOrClientName}' via a custom policy")
          result
        })
      case EmptyPolicy => Directive.Empty
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
  def apply(implicit ec: ExecutionContext): SecurityDirectives = {
    val authConfig                             = AuthConfig.loadFromAppConfig
    val keycloakDeployment: KeycloakDeployment = authConfig.getDeployment
    val authentication                         = new Authentication(new TokenFactory(keycloakDeployment))
    new SecurityDirectives(authentication, keycloakDeployment.getRealm, keycloakDeployment.getResourceName)
  }

  def apply(locationService: LocationService)(implicit ec: ExecutionContext): SecurityDirectives = {
    //todo: see if its possible to remove blocking here
    val authLocation: HttpLocation             = Await.result(AuthServiceLocation(locationService).resolve, 5.seconds)
    val authConfig                             = AuthConfig.loadFromAppConfig(authLocation)
    val keycloakDeployment: KeycloakDeployment = authConfig.getDeployment
    val authentication                         = new Authentication(new TokenFactory(keycloakDeployment))
    new SecurityDirectives(authentication, keycloakDeployment.getRealm, keycloakDeployment.getResourceName)
  }
}
