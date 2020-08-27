package csw.aas.installed

import com.typesafe.config.{Config, ConfigFactory}
import csw.aas.core.TokenVerifier
import csw.aas.core.deployment.{AuthConfig, AuthServiceLocation}
import csw.aas.installed.api.{AuthStore, InstalledAppAuthAdapter}
import csw.aas.installed.internal.InstalledAppAuthAdapterImpl
import csw.location.api.models.HttpLocation
import csw.location.api.scaladsl.LocationService
import org.keycloak.adapters.installed.KeycloakInstalled

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}
import scala.util.Try

object InstalledAppAuthAdapterFactory {

  /**
   *  Creates an instance of InstalledAppAuthAdapter. Resolves authentication service using location service and ignores `auth-server-url` config parameter
   *
   * @param config for InstalledAppAuthAdapter
   * @param locationService handle to [[csw.location.api.scaladsl.LocationService]] used to resolve location of aas
   * @param secretStore store where all tokens will be stored. This library provides implementation for [[csw.aas.installed.scaladsl.FileAuthStore]]
   *                    but one can choose to implement their own [[csw.aas.installed.api.AuthStore]] and plug it in here.
   * @param executionContext require for execution of asynchronous task
   * @return handle to [[csw.aas.installed.api.InstalledAppAuthAdapter]] with which you can login, logout and get access tokens
   */
  def make(config: Config, locationService: LocationService, secretStore: AuthStore)(implicit
      executionContext: ExecutionContext
  ): InstalledAppAuthAdapter = make(locationService, Some(secretStore), config)

  /**
   * Creates an instance of InstalledAppAuthAdapter. Resolves authentication service using location service and ignores `auth-server-url` config parameter
   *
   * @param locationService   handle to [[csw.location.api.scaladsl.LocationService]] used to resolve location of aas
   * @param secretStore       store where all tokens will be stored. This library provides implementation for [[csw.aas.installed.scaladsl.FileAuthStore]]
   *                          but one can choose to implement their own [[csw.aas.installed.api.AuthStore]] and plug it in here.
   * @param executionContext  require for execution of asynchronous task
   * @return handle to [[csw.aas.installed.api.InstalledAppAuthAdapter]] with which you can login, logout and get access tokens
   */
  def make(locationService: LocationService, secretStore: AuthStore)(implicit
      executionContext: ExecutionContext
  ): InstalledAppAuthAdapter = make(locationService, Some(secretStore))

  /**
   * Creates an instance of InstalledAppAuthAdapter. Resolves authentication service using location service and ignores `auth-server-url` config parameter.
   * Uses the default in memory auth store for storing all tokens
   *
   * @param locationService   handle to [[csw.location.api.scaladsl.LocationService]] used to resolve location of aas
   * @param executionContext  require for execution of asynchronous task
   * @return handle to [[csw.aas.installed.api.InstalledAppAuthAdapter]] with which you can login, logout and get access tokens
   */
  def make(locationService: LocationService)(implicit executionContext: ExecutionContext): InstalledAppAuthAdapter =
    make(locationService, None)

  /**
   * ****************
   *  INTERNAL APIs
   * ****************
   */
  private def make(locationService: LocationService, secretStore: Option[AuthStore], config: Config = ConfigFactory.load())(
      implicit executionContext: ExecutionContext
  ): InstalledAppAuthAdapter = {
    val authConfig    = makeAuthConfig(locationService, config)
    val tokenVerifier = TokenVerifier(authConfig)
    new InstalledAppAuthAdapterImpl(authConfig, new KeycloakInstalled(authConfig.getDeployment), tokenVerifier, secretStore)
  }

  private def disabled(config: Config): Boolean = {
    val mayBeValue = Try { config.getConfig(AuthConfig.authConfigKey).getBoolean(AuthConfig.disabledKey) }.toOption
    mayBeValue.nonEmpty && mayBeValue.get
  }

  private def authLocation(locationService: LocationService)(implicit ec: ExecutionContext): HttpLocation =
    Await.result(AuthServiceLocation(locationService).resolve(5.seconds), 6.seconds)

  private def makeAuthConfig(locationService: LocationService, config: Config)(implicit
      executionContext: ExecutionContext
  ) = {
    val maybeLocation: Option[HttpLocation] = if (disabled(config)) None else Some(authLocation(locationService))
    AuthConfig(config, maybeLocation)
  }
}
