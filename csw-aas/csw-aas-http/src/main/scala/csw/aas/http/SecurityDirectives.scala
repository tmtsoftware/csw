/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.aas.http

import org.apache.pekko.http.scaladsl.model.HttpMethods._
import org.apache.pekko.http.scaladsl.server._
import com.typesafe.config.Config
import csw.aas.core.TokenVerifier
import csw.aas.core.commons.AuthLogger
import csw.aas.core.deployment.{AuthConfig, AuthServiceLocation}
import csw.aas.core.token.TokenFactory
import csw.aas.core.utils.ConfigExt._
import csw.location.api.models.HttpLocation
import csw.location.api.scaladsl.LocationService
import msocket.security.AccessControllerFactory
import msocket.security.api.AuthorizationPolicy
import msocket.security.models.AccessToken

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.implicitConversions

class SecurityDirectives private[csw] (policyValidator: PolicyValidator) {
  implicit def toRouteFunction(route: Route): AccessToken => Route                            = _ => route
  implicit def toBooleanFunction(bool: Boolean): AccessToken => Boolean                       = _ => bool
  implicit def toBooleanFutureFunction(bool: Future[Boolean]): AccessToken => Future[Boolean] = _ => bool

  /**
   * Rejects all un-authorized and non-POST requests
   *
   * @param authorizationPolicy Authorization policy to use for filtering requests.
   *                            There are different types of authorization policies. See [[csw.aas.http.AuthorizationPolicy]]
   */
  def sPost(authorizationPolicy: AuthorizationPolicy): Directive1[AccessToken] =
    policyValidator.validate(POST, authorizationPolicy)

  /**
   * Rejects all un-authorized and non-GET requests
   *
   * @param authorizationPolicy Authorization policy to use for filtering requests.
   *                            There are different types of authorization policies. See [[csw.aas.http.AuthorizationPolicy]]
   */
  def sGet(authorizationPolicy: AuthorizationPolicy): Directive1[AccessToken] = policyValidator.validate(GET, authorizationPolicy)

  /**
   * Rejects all un-authorized and non-GET requests
   *
   * @param authorizationPolicy Authorization policy to use for filtering requests.
   *                            There are different types of authorization policies. See [[csw.aas.http.AuthorizationPolicy]]
   */
  def sPut(authorizationPolicy: AuthorizationPolicy): Directive1[AccessToken] = policyValidator.validate(PUT, authorizationPolicy)

  /**
   * Rejects all un-authorized and non-PUT requests
   *
   * @param authorizationPolicy Authorization policy to use for filtering requests.
   *                            There are different types of authorization policies. See [[csw.aas.http.AuthorizationPolicy]]
   */
  def sDelete(authorizationPolicy: AuthorizationPolicy): Directive1[AccessToken] =
    policyValidator.validate(DELETE, authorizationPolicy)

  /**
   * Rejects all un-authorized and non-PATCH requests
   *
   * @param authorizationPolicy Authorization policy to use for filtering requests.
   *                            There are different types of authorization policies. See [[csw.aas.http.AuthorizationPolicy]]
   */
  def sPatch(authorizationPolicy: AuthorizationPolicy): Directive1[AccessToken] =
    policyValidator.validate(PATCH, authorizationPolicy)

  /**
   * Rejects all un-authorized and non-HEAD requests
   *
   * @param authorizationPolicy Authorization policy to use for filtering requests.
   *                            There are different types of authorization policies. See [[csw.aas.http.AuthorizationPolicy]]
   */
  def sHead(authorizationPolicy: AuthorizationPolicy): Directive1[AccessToken] =
    policyValidator.validate(HEAD, authorizationPolicy)

  /**
   * Rejects all un-authorized and non-CONNECT requests
   *
   * @param authorizationPolicy Authorization policy to use for filtering requests.
   *                            There are different types of authorization policies. See [[csw.aas.http.AuthorizationPolicy]]
   */
  def sConnect(authorizationPolicy: AuthorizationPolicy): Directive1[AccessToken] =
    policyValidator.validate(CONNECT, authorizationPolicy)

}

/**
 * Factory for [[csw.aas.http.SecurityDirectives]] instances
 */
object SecurityDirectives {

  /**
   * Creates instance of [[csw.aas.http.SecurityDirectives]] using provided configurations
   * and resolves auth server url using location service
   *
   * @param config Config object provided
   * @param locationService LocationService instance used to resolve auth server url (blocking call)
   */
  def apply(config: Config, locationService: LocationService)(implicit ec: ExecutionContext): SecurityDirectives = {
    from(AuthConfig(config, mayBeLocation(enableAuthUsing(config), locationService)))
  }

  /**
   * Creates instance of [[csw.aas.http.SecurityDirectives]] using provided configurations
   * and resolves auth server url using location service
   *
   * @param config Config object provided
   * @param locationService LocationService instance used to resolve auth server url (blocking call)
   * @param enableAuth It will ignore `disabled` key from config. This can be used by cli apps which wants to enable/disable auth
   *                   based on whether they are started with auth enabled or auth disabled rather than relying on config
   */
  def apply(config: Config, locationService: LocationService, enableAuth: Boolean)(implicit
      ec: ExecutionContext
  ): SecurityDirectives = {
    from(AuthConfig(config, mayBeLocation(enableAuth, locationService)))
  }

  /**
   * Creates instance of [[csw.aas.http.SecurityDirectives]] with auth disabled
   */
  def authDisabled(config: Config)(implicit ec: ExecutionContext): SecurityDirectives =
    from(AuthConfig(config, None))

  private def from(authConfig: AuthConfig)(implicit ec: ExecutionContext): SecurityDirectives = {
    val keycloakDeployment = authConfig.getDeployment
    val tokenFactory       = new TokenFactory(TokenVerifier(authConfig))
    val securityEnabled    = !authConfig.disabled
    if (!securityEnabled) {
      AuthLogger.getLogger.warn(
        "Security directives initialized with auth disabled. " +
          "All un-authorised calls will be granted access"
      )
    }
    val accessControllerFactory = new AccessControllerFactory(tokenFactory, securityEnabled)
    val policyValidator         = new PolicyValidator(accessControllerFactory, keycloakDeployment.getRealm)
    new SecurityDirectives(policyValidator)
  }

  private def enableAuthUsing(config: Config): Boolean =
    !config.getConfig(AuthConfig.authConfigKey).getBooleanOrFalse(AuthConfig.disabledKey)

  private def mayBeLocation(enableAuth: Boolean, locationService: LocationService)(implicit
      ec: ExecutionContext
  ): Option[HttpLocation] = {
    if (enableAuth) Some(authLocation(locationService)) else None
  }

  private def authLocation(locationService: LocationService)(implicit ec: ExecutionContext): HttpLocation =
    Await.result(AuthServiceLocation(locationService).resolve(5.seconds), 6.seconds)
}
