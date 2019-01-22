package csw.aas.native

import csw.aas.core.TokenVerifier
import csw.aas.core.deployment.{AuthConfig, AuthServiceLocation}
import csw.aas.native.api.{AuthStore, NativeAppAuthAdapter}
import csw.aas.native.internal.NativeAppAuthAdapterImpl
import csw.location.api.scaladsl.LocationService
import org.keycloak.adapters.installed.KeycloakInstalled

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}

object NativeAppAuthAdapterFactory {

  /**
   * Creates an instance of NativeAppAuthAdapter. Resolves authentication service using location service and ignores `auth-server-url` config parameter
   *
   * @param locationService   handle to [[LocationService]] used to resolve location of aas
   * @param secretStore       store where all tokens will be stored. This library provides implementation for [[csw.aas.native.scaladsl.FileAuthStore]]
   *                          but one can choose to implement their own [[AuthStore]] and plug it in here.
   * @param executionContext  require for execution of asynchronous task
   * @return handle to [[NativeAppAuthAdapter]] with which you can login, logout and get access tokens
   */
  def make(locationService: LocationService, secretStore: AuthStore)(
      implicit executionContext: ExecutionContext
  ): NativeAppAuthAdapter = make(locationService, Some(secretStore))

  /**
   * Creates an instance of NativeAppAuthAdapter. Resolves authentication service using location service and ignores `auth-server-url` config parameter.
   * Uses the default in memory auth store for storing all tokens
   *
   * @param locationService   handle to [[LocationService]] used to resolve location of aas
   * @param executionContext  require for execution of asynchronous task
   * @return handle to [[NativeAppAuthAdapter]] with which you can login, logout and get access tokens
   */
  def make(locationService: LocationService)(implicit executionContext: ExecutionContext): NativeAppAuthAdapter =
    make(locationService, None)

  /**
   * Creates an instance of NativeAppAuthAdapter. Does not resolve authentication service using location service. Instead it uses "auth-config.auth-server-url"
   * config parameter to resolve authentication service
   *
   * @param secretStore store where all tokens will be stored.
   *                    This library provides implementation for [[csw.aas.native.scaladsl.FileAuthStore]]
   *                    but one can choose to implement their own [[AuthStore]] and plug it in here.
   * @return handle to [[NativeAppAuthAdapter]] with which you can login, logout and get access tokens
   */
  def make(secretStore: AuthStore)(implicit executionContext: ExecutionContext): NativeAppAuthAdapter = make(Some(secretStore))

  /**
   * Creates an instance of NativeAppAuthAdapter. Does not resolve authentication service using location service. Instead it uses "auth-config.auth-server-url"
   * config parameter to resolve authentication service. Uses the default in memory [[AuthStore]] for storing all tokens
   *
   * @return handle to [[NativeAppAuthAdapter]] with which you can login, logout and get access tokens
   */
  def make(implicit executionContext: ExecutionContext): NativeAppAuthAdapter = make(None)

  /******************
   *  INTERNAL APIs
   ******************/
  private def make(locationService: LocationService, secretStore: Option[AuthStore])(
      implicit executionContext: ExecutionContext
  ): NativeAppAuthAdapter = {
    val authServiceLocation = Await.result(AuthServiceLocation(locationService).resolve(5.seconds), 10.seconds)
    val authConfig          = AuthConfig.create(authServerLocation = Some(authServiceLocation))
    val tokenVerifier       = TokenVerifier(authConfig)
    new NativeAppAuthAdapterImpl(new KeycloakInstalled(authConfig.getDeployment), tokenVerifier, secretStore)
  }

  private def make(secretStore: Option[AuthStore])(implicit executionContext: ExecutionContext): NativeAppAuthAdapter = {
    val authConfig    = AuthConfig.create()
    val tokenVerifier = TokenVerifier(authConfig)
    new NativeAppAuthAdapterImpl(new KeycloakInstalled(authConfig.getDeployment), tokenVerifier, secretStore)
  }
}
