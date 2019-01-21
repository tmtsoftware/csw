package csw.aas.http

import akka.http.scaladsl.model.HttpMethod
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.server.Directives.{authorize => keycloakAuthorize, _}
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.AuthenticationDirective
import com.typesafe.config.Config
import csw.aas.core.TokenVerifier
import csw.aas.core.commons.AuthLogger
import csw.aas.core.deployment.{AuthConfig, AuthServiceLocation}
import csw.aas.core.token.{AccessToken, TokenFactory}
import csw.aas.http.AuthorizationPolicy.{EmptyPolicy, _}
import csw.location.api.scaladsl.LocationService

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, ExecutionContext}
import scala.language.implicitConversions

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
        keycloakAuthorize {
          val result = predicate(accessToken)
          if (!result) debug(s"'${accessToken.userOrClientName}' failed custom policy authorization")
          else debug(s"authorization succeeded for '${accessToken.userOrClientName}' via a custom policy")
          result
        }
      case EmptyPolicy => Directive.Empty
    }

  private def sMethod(httpMethod: HttpMethod, authorizationPolicy: AuthorizationPolicy): Directive1[AccessToken] =
    method(httpMethod) & authenticate.flatMap(token => authorize(authorizationPolicy, token) & provide(token))

  /**
   * Rejects all un-authorized and non-POST requests
   *
   * @param authorizationPolicy Authorization policy to use for filtering requests.
   *                            There are different types of authorization policies. See [[csw.aas.http.AuthorizationPolicy]]
   */
  def sPost(authorizationPolicy: AuthorizationPolicy): Directive1[AccessToken] = sMethod(POST, authorizationPolicy)

  /**
   * Rejects all un-authorized and non-GET requests
   *
   * @param authorizationPolicy Authorization policy to use for filtering requests.
   *                            There are different types of authorization policies. See [[csw.aas.http.AuthorizationPolicy]]
   */
  def sGet(authorizationPolicy: AuthorizationPolicy): Directive1[AccessToken] = sMethod(GET, authorizationPolicy)

  /**
   * Rejects all un-authorized and non-GET requests
   * @param authorizationPolicy Authorization policy to use for filtering requests.
   *                            There are different types of authorization policies. See [[csw.aas.http.AuthorizationPolicy]]
   */
  def sPut(authorizationPolicy: AuthorizationPolicy): Directive1[AccessToken] = sMethod(PUT, authorizationPolicy)

  /**
   * Rejects all un-authorized and non-PUT requests
   * @param authorizationPolicy Authorization policy to use for filtering requests.
   *                            There are different types of authorization policies. See [[csw.aas.http.AuthorizationPolicy]]
   */
  def sDelete(authorizationPolicy: AuthorizationPolicy): Directive1[AccessToken] = sMethod(DELETE, authorizationPolicy)

  /**
   * Rejects all un-authorized and non-PATCH requests
   * @param authorizationPolicy Authorization policy to use for filtering requests.
   *                            There are different types of authorization policies. See [[csw.aas.http.AuthorizationPolicy]]
   */
  def sPatch(authorizationPolicy: AuthorizationPolicy): Directive1[AccessToken] = sMethod(PATCH, authorizationPolicy)

  /**
   * Rejects all un-authorized and non-HEAD requests
   * @param authorizationPolicy Authorization policy to use for filtering requests.
   *                            There are different types of authorization policies. See [[csw.aas.http.AuthorizationPolicy]]
   */
  def sHead(authorizationPolicy: AuthorizationPolicy): Directive1[AccessToken] = sMethod(HEAD, authorizationPolicy)

  /**
   * Rejects all un-authorized and non-CONNECT requests
   * @param authorizationPolicy Authorization policy to use for filtering requests.
   *                            There are different types of authorization policies. See [[csw.aas.http.AuthorizationPolicy]]
   */
  def sConnect(authorizationPolicy: AuthorizationPolicy): Directive1[AccessToken] = sMethod(CONNECT, authorizationPolicy)
}

/**
 * Factory for [[csw.aas.http.SecurityDirectives]] instances
 */
object SecurityDirectives {

  /**
   * Creates instance of [[csw.aas.http.SecurityDirectives]] using configurations
   * from application and reference.conf.
   *
   * Expects auth-server-url to be present in config.
   * @return
   */
  def apply(implicit ec: ExecutionContext): SecurityDirectives = from(AuthConfig.create())

  /**
   * Creates instance of [[csw.aas.http.SecurityDirectives]] using configurations
   * from application and reference.conf.
   *
   * Resolves auth server url using location service (blocking call)
   */
  def apply(locationService: LocationService)(implicit ec: ExecutionContext): SecurityDirectives =
    from(AuthConfig.create(authServerLocation = Some(authLocation(locationService))))

  /**
   * Creates instance of [[csw.aas.http.SecurityDirectives]] using configurations
   * from application and reference.conf.
   *
   * Resolves auth server url using location service (blocking call)
   */
  def apply(config: Config, locationService: LocationService)(implicit ec: ExecutionContext): SecurityDirectives =
    from(AuthConfig.create(config, Some(authLocation(locationService))))

  //todo: see if its possible to remove blocking here
  private def authLocation(locationService: LocationService)(implicit ec: ExecutionContext) =
    Await.result(AuthServiceLocation(locationService).resolve(5.seconds), 10.seconds)

  private def from(authConfig: AuthConfig)(implicit ec: ExecutionContext): SecurityDirectives = {
    val keycloakDeployment = authConfig.getDeployment
    val tokenVerifier      = TokenVerifier(authConfig)
    val authentication     = new Authentication(new TokenFactory(keycloakDeployment, tokenVerifier, authConfig.permissionsEnabled))
    new SecurityDirectives(authentication, keycloakDeployment.getRealm, keycloakDeployment.getResourceName)
  }
}
