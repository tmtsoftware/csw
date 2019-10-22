package csw.aas.installed

import com.typesafe.config.{Config, ConfigFactory}
import csw.aas.core.TokenVerifier
import csw.aas.core.deployment.{AuthConfig, AuthServiceLocation}
import csw.aas.installed.api.{AuthStore, InstalledAppAuthAdapter}
import csw.aas.installed.internal.InstalledAppAuthAdapterImpl
import csw.location.api.scaladsl.LocationService
import org.keycloak.adapters.installed.KeycloakInstalled

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.DurationInt

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
  def make(config: Config, locationService: LocationService, secretStore: AuthStore)(
      implicit executionContext: ExecutionContext
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
  def make(locationService: LocationService, secretStore: AuthStore)(
      implicit executionContext: ExecutionContext
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
   * Creates an instance of InstalledAppAuthAdapter. Does not resolve authentication service using location service. Instead it uses "auth-config.auth-server-url"
   * config parameter to resolve authentication service
   *
   * @param secretStore store where all tokens will be stored.
   *                    This library provides implementation for [[csw.aas.installed.scaladsl.FileAuthStore]]
   *                    but one can choose to implement their own [[csw.aas.installed.api.AuthStore]] and plug it in here.
   * @return handle to [[csw.aas.installed.api.InstalledAppAuthAdapter]] with which you can login, logout and get access tokens
   */
  def make(secretStore: AuthStore)(implicit executionContext: ExecutionContext): InstalledAppAuthAdapter = make(Some(secretStore))

  /**
   * Creates an instance of InstalledAppAuthAdapter. Does not resolve authentication service using location service. Instead it uses "auth-config.auth-server-url"
   * config parameter to resolve authentication service. Uses the default in memory [[csw.aas.installed.api.AuthStore]] for storing all tokens
   *
   * @return handle to [[csw.aas.installed.api.InstalledAppAuthAdapter]] with which you can login, logout and get access tokens
   */
  def make(implicit executionContext: ExecutionContext): InstalledAppAuthAdapter = make(None)

  /******************
   *  INTERNAL APIs
   ******************/
  private def make(locationService: LocationService, secretStore: Option[AuthStore], config: Config = ConfigFactory.load())(
      implicit executionContext: ExecutionContext
  ): InstalledAppAuthAdapter = {
    val authServiceLocationF = Await.result(AuthServiceLocation(locationService).resolve(5.seconds), 6.seconds)
    val authConfig           = AuthConfig.create(config = config, authServerLocation = Some(authServiceLocationF))
    val tokenVerifier        = TokenVerifier(authConfig)
    new InstalledAppAuthAdapterImpl(authConfig, new KeycloakInstalled(authConfig.getDeployment), tokenVerifier, secretStore)
  }

  private def make(secretStore: Option[AuthStore])(implicit executionContext: ExecutionContext): InstalledAppAuthAdapter = {
    val authConfig    = AuthConfig.create()
    val tokenVerifier = TokenVerifier(authConfig)
    new InstalledAppAuthAdapterImpl(authConfig, new KeycloakInstalled(authConfig.getDeployment), tokenVerifier, secretStore)
  }
}
