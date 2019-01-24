package csw.aas.http

import akka.http.scaladsl.model.HttpMethod
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.server.Directives.{authorize => keycloakAuthorize, authorizeAsync => keycloakAuthorizeAsync, _}
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.AuthenticationDirective
import com.typesafe.config.Config
import csw.aas.core.TokenVerifier
import csw.aas.core.commons.AuthLogger
import csw.aas.core.deployment.{AuthConfig, AuthServiceLocation}
import csw.aas.core.token.{AccessToken, TokenFactory}
import csw.aas.http.AuthorizationPolicy.PolicyExpression.{And, Or}
import csw.aas.http.AuthorizationPolicy.{EmptyPolicy, _}
import csw.location.api.scaladsl.LocationService

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.implicitConversions
import scala.util.{Failure, Success}

class SecurityDirectives private[csw] (authentication: Authentication, realm: String, resourceName: String)(
    implicit ec: ExecutionContext
) {

  private val logger = AuthLogger.getLogger
  import logger._

  implicit def toRouteFunction(route: Route): AccessToken => Route                            = _ => route
  implicit def toBooleanFunction(bool: Boolean): AccessToken => Boolean                       = _ => bool
  implicit def toBooleanFutureFunction(bool: Future[Boolean]): AccessToken => Future[Boolean] = _ => bool

  private[aas] def authenticate: AuthenticationDirective[AccessToken] =
    authenticateOAuth2Async(realm, authentication.authenticator)

  private[aas] def authorize(authorizationPolicy: AuthorizationPolicy, accessToken: AccessToken): Directive0 =
    authorizationPolicy match {
      case ClientRolePolicy(name)           => keycloakAuthorize(accessToken.hasClientRole(name, resourceName))
      case RealmRolePolicy(name)            => keycloakAuthorize(accessToken.hasRealmRole(name))
      case PermissionPolicy(name, resource) => keycloakAuthorize(accessToken.hasPermission(name, resource))
      case CustomPolicy(predicate) =>
        keycloakAuthorize {
          val result = predicate(accessToken)
          if (!result) debug(s"'${accessToken.userOrClientName}' failed custom policy authorization")
          else debug(s"authorization succeeded for '${accessToken.userOrClientName}' via a custom policy")
          result
        }
      case CustomPolicyAsync(predicate) =>
        keycloakAuthorizeAsync {
          val result = predicate(accessToken)
          result.onComplete {
            case Success(authorized) =>
              if (authorized) {
                debug(s"authorization succeeded for '${accessToken.userOrClientName}' via a custom policy")
              } else {
                warn(s"'${accessToken.userOrClientName}' failed custom policy authorization")
              }
            case Failure(exception) =>
              error(s"error while executing async custom policy for ${accessToken.userOrClientName}", ex = exception)
          }
          result
        }
      case EmptyPolicy => Directive.Empty
      case PolicyExpression(left, op, right) =>
        op match {
          case And => authorize(left, accessToken) & authorize(right, accessToken)
          case Or  => authorize(left, accessToken) | authorize(right, accessToken)
        }
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
   * @param locationService LocationService instance used to resolve auth server url (blocking call)
   * Resolves auth server url using location service (blocking call)
   */
  def apply(locationService: LocationService)(implicit ec: ExecutionContext): SecurityDirectives =
    from(AuthConfig.create(authServerLocation = Some(authLocation(locationService))))

  /**
   * Creates instance of [[csw.aas.http.SecurityDirectives]] using provided configurations
   * and auth server url using location service
   *
   * @param config Config object provided
   * @param locationService LocationService instance used to resolve auth server url (blocking call)
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
