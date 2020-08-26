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
import csw.aas.core.utils.ConfigExt._
import csw.aas.http.AuthorizationPolicy._
import csw.location.api.models.HttpLocation
import csw.location.api.scaladsl.LocationService

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.implicitConversions
import scala.util.{Failure, Success}

class SecurityDirectives private[csw] (
    authentication: Authentication,
    realm: String,
    disabled: Boolean
)(implicit
    ec: ExecutionContext
) {

  private val logger = AuthLogger.getLogger

  if (disabled)
    logger.warn(
      "Security directives initialized with auth disabled. " +
        "All un-authorised calls will be granted access"
    )

  implicit def toRouteFunction(route: Route): AccessToken => Route                            = _ => route
  implicit def toBooleanFunction(bool: Boolean): AccessToken => Boolean                       = _ => bool
  implicit def toBooleanFutureFunction(bool: Future[Boolean]): AccessToken => Future[Boolean] = _ => bool

  /**
   * Rejects all un-authorized requests
   * @param authorizationPolicy Authorization policy to use for filtering requests.
   *                            There are different types of authorization policies. See [[csw.aas.http.AuthorizationPolicy]]
   */
  def secure(authorizationPolicy: AuthorizationPolicy): Directive1[AccessToken] = {
    if (!disabled)
      authenticate.flatMap(token => authorize(authorizationPolicy, token) & provide(token))
    else
      provide(AccessToken())
  }

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
   *
   * @param authorizationPolicy Authorization policy to use for filtering requests.
   *                            There are different types of authorization policies. See [[csw.aas.http.AuthorizationPolicy]]
   */
  def sPut(authorizationPolicy: AuthorizationPolicy): Directive1[AccessToken] = sMethod(PUT, authorizationPolicy)

  /**
   * Rejects all un-authorized and non-PUT requests
   *
   * @param authorizationPolicy Authorization policy to use for filtering requests.
   *                            There are different types of authorization policies. See [[csw.aas.http.AuthorizationPolicy]]
   */
  def sDelete(authorizationPolicy: AuthorizationPolicy): Directive1[AccessToken] = sMethod(DELETE, authorizationPolicy)

  /**
   * Rejects all un-authorized and non-PATCH requests
   *
   * @param authorizationPolicy Authorization policy to use for filtering requests.
   *                            There are different types of authorization policies. See [[csw.aas.http.AuthorizationPolicy]]
   */
  def sPatch(authorizationPolicy: AuthorizationPolicy): Directive1[AccessToken] = sMethod(PATCH, authorizationPolicy)

  /**
   * Rejects all un-authorized and non-HEAD requests
   *
   * @param authorizationPolicy Authorization policy to use for filtering requests.
   *                            There are different types of authorization policies. See [[csw.aas.http.AuthorizationPolicy]]
   */
  def sHead(authorizationPolicy: AuthorizationPolicy): Directive1[AccessToken] = sMethod(HEAD, authorizationPolicy)

  /**
   * Rejects all un-authorized and non-CONNECT requests
   *
   * @param authorizationPolicy Authorization policy to use for filtering requests.
   *                            There are different types of authorization policies. See [[csw.aas.http.AuthorizationPolicy]]
   */
  def sConnect(authorizationPolicy: AuthorizationPolicy): Directive1[AccessToken] = sMethod(CONNECT, authorizationPolicy)

  private[aas] def authenticate: AuthenticationDirective[AccessToken] =
    authenticateOAuth2Async(realm, authentication.authenticator)

  private[aas] def authorize(authorizationPolicy: AuthorizationPolicy, accessToken: AccessToken): Directive0 =
    authorizationPolicy match {
      case RealmRolePolicy(name) => keycloakAuthorize(accessToken.hasRealmRole(name))
      case CustomPolicy(predicate) =>
        keycloakAuthorize {
          val result = predicate(accessToken)
          if (!result) logger.debug(s"'${accessToken.userOrClientName}' failed custom policy authorization")
          else logger.debug(s"authorization succeeded for '${accessToken.userOrClientName}' via a custom policy")
          result
        }
      case CustomPolicyAsync(predicate) =>
        keycloakAuthorizeAsync {
          val result = predicate(accessToken)
          result.onComplete {
            case Success(authorized) =>
              if (authorized) {
                logger.debug(s"authorization succeeded for '${accessToken.userOrClientName}' via a custom policy")
              }
              else {
                logger.warn(s"'${accessToken.userOrClientName}' failed custom policy authorization")
              }
            case Failure(exception) =>
              logger.error(s"error while executing async custom policy for ${accessToken.userOrClientName}", ex = exception)
          }
          result
        }
      case AndPolicy(left, right) => authorize(left, accessToken) & authorize(right, accessToken)
      case OrPolicy(left, right)  => authorize(left, accessToken) | authorize(right, accessToken)
    }

  private def sMethod(httpMethod: HttpMethod, authorizationPolicy: AuthorizationPolicy): Directive1[AccessToken] =
    method(httpMethod) & secure(authorizationPolicy)
}

/**
 * Factory for [[csw.aas.http.SecurityDirectives]] instances
 */
object SecurityDirectives {

  /**
   * Creates instance of [[csw.aas.http.SecurityDirectives]] using provided configurations
   *
   * Expects auth-server-url to be present in config.
   *
   * @param config Config object provided
   */
  def apply(config: Config)(implicit ec: ExecutionContext): SecurityDirectives =
    from(AuthConfig.create(config, None))

  /**
   * Creates instance of [[csw.aas.http.SecurityDirectives]] using provided configurations
   * and auth server url using location service
   *
   * @param config Config object provided
   * @param locationService LocationService instance used to resolve auth server url (blocking call)
   */
  def apply(config: Config, locationService: LocationService)(implicit ec: ExecutionContext): SecurityDirectives = {
    val maybeLocation = if (disabled(config)) None else Some(authLocation(locationService))
    from(AuthConfig.create(config, maybeLocation))
  }

  /**
   * Creates instance of [[csw.aas.http.SecurityDirectives]] using proivided configurations
   *
   * @param locationService LocationService instance used to resolve auth server url (blocking call)
   * Resolves auth server url using location service (blocking call)
   * @param disabled if explicitly disabled/enabled, it will ignore `disabled` key from config
   */
  private[csw] def apply(config: Config, locationService: LocationService, disabled: Boolean)(implicit
      ec: ExecutionContext
  ): SecurityDirectives = {
    val maybeLocation = if (disabled) None else Some(authLocation(locationService))
    from(AuthConfig.create(config, maybeLocation, Some(disabled)))
  }

  /**
   * Creates instance of [[csw.aas.http.SecurityDirectives]] with auth disabled
   */
  def authDisabled(config: Config)(implicit ec: ExecutionContext): SecurityDirectives =
    from(AuthConfig.create(config = config, authServerLocation = None, disabledMaybe = Some(true)))

  private def from(authConfig: AuthConfig)(implicit ec: ExecutionContext): SecurityDirectives = {
    val keycloakDeployment = authConfig.getDeployment
    val tokenVerifier      = TokenVerifier(authConfig)
    val authentication     = new Authentication(new TokenFactory(tokenVerifier))
    new SecurityDirectives(authentication, keycloakDeployment.getRealm, authConfig.disabled)
  }

  private def disabled(config: Config): Boolean =
    config.getConfig(AuthConfig.authConfigKey).getBooleanOrFalse(AuthConfig.disabledKey)

  private def authLocation(locationService: LocationService)(implicit ec: ExecutionContext): HttpLocation =
    Await.result(AuthServiceLocation(locationService).resolve(5.seconds), 6.seconds)
}
