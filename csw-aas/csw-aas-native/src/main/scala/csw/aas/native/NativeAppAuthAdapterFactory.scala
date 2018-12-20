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
   * @param locationService
   * @param secretStore The store where all tokens will be stored. If you don't provide this a default file auth store will be used
   * @param executionContext
   * @return
   */
  def make(locationService: LocationService, secretStore: AuthStore)(
      implicit executionContext: ExecutionContext
  ): NativeAppAuthAdapter = make(locationService, Some(secretStore))

  /**
   * Creates an instance of NativeAppAuthAdapter. Resolves authentication service using location service and ignores `auth-server-url` config parameter.
   * Uses the default file auth store for storing all tokens
   * @param locationService
   * @param executionContext
   * @return
   */
  def make(locationService: LocationService)(implicit executionContext: ExecutionContext): NativeAppAuthAdapter =
    make(locationService, None)

  /**
   * Creates an instance of NativeAppAuthAdapter. Does not resolve authentication service using location service. Instead it uses "auth-config.auth-server-url"
   * config parameter to resolve authentication service
   * @param secretStore The store where all tokens will be stored. If you don't provide this a default file auth store will be used
   * @return
   */
  def make(secretStore: AuthStore)(implicit executionContext: ExecutionContext): NativeAppAuthAdapter = make(Some(secretStore))

  /**
   * Creates an instance of NativeAppAuthAdapter. Does not resolve authentication service using location service. Instead it uses "auth-config.auth-server-url"
   * config parameter to resolve authentication service. Uses the default file auth store for storing all tokens
   * @return
   */
  def make(implicit executionContext: ExecutionContext): NativeAppAuthAdapter = make(None)

  private def make(locationService: LocationService, secretStore: Option[AuthStore])(
      implicit executionContext: ExecutionContext
  ): NativeAppAuthAdapter = {
    val authServiceLocation = Await.result(AuthServiceLocation(locationService).resolve(5.seconds), 10.seconds)
    val authConfig          = AuthConfig.loadFromAppConfig(Some(authServiceLocation))
    val tokenVerifier       = TokenVerifier(authConfig)
    new NativeAppAuthAdapterImpl(new KeycloakInstalled(authConfig.getDeployment), tokenVerifier, secretStore)
  }

  private def make(secretStore: Option[AuthStore])(implicit executionContext: ExecutionContext): NativeAppAuthAdapter = {
    val authConfig    = AuthConfig.loadFromAppConfig()
    val tokenVerifier = TokenVerifier(authConfig)
    new NativeAppAuthAdapterImpl(new KeycloakInstalled(authConfig.getDeployment), tokenVerifier, secretStore)
  }
}
