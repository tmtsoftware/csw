package csw.aas.core.deployment
import java.io.{ByteArrayInputStream, InputStream}

import com.typesafe.config._
import csw.aas.core.deployment.AuthConfig._
import csw.location.api.models.HttpLocation
import org.keycloak.adapters.{KeycloakDeployment, KeycloakDeploymentBuilder}

/**
 * Represents Authorization configuration
 *
 * @param config application config
 * @param authServiceLocation if authServiceLocation is provided, it will use it and treat it as auth enabled
 *                            otherwise treat it as auth disabled
 */
class AuthConfig private (config: Config, authServiceLocation: Option[HttpLocation]) {

  private val clientIdKey = "client-id"
  val disabled: Boolean   = authServiceLocation.isEmpty

  /**
   * Creates an instance of KeycloakDeployment using app config.
   * if authServiceLocation is provided, it will use it and treat it as auth enabled
   * otherwise treat it as auth disabled
   *
   * @return
   */
  private[csw] def getDeployment: KeycloakDeployment =
    authServiceLocation match {
      case Some(location) =>
        val configWithResolvedAuthUrl = config.withValue("auth-server-url", ConfigValueFactory.fromAnyRef(location.uri.toString))
        convertToDeployment(configWithResolvedAuthUrl)
      case None =>
        val configForDisabledAuth =
          config.withValue("auth-server-url", ConfigValueFactory.fromAnyRef("http://disabled-auth-service"))
        convertToDeployment(configForDisabledAuth)
    }

  private def convertToDeployment(config: Config): KeycloakDeployment = {
    val clientId = config.getString(clientIdKey)
    val safeConfig = config
      .withoutPath(disabledKey)
      .withoutPath(clientIdKey)
      .withValue("resource", ConfigValueFactory.fromAnyRef(clientId))
    val configJSON: String             = safeConfig.root().render(ConfigRenderOptions.concise())
    val inputStream: InputStream       = new ByteArrayInputStream(configJSON.getBytes())
    val deployment: KeycloakDeployment = KeycloakDeploymentBuilder.build(inputStream)
    deployment
  }
}

object AuthConfig {

  private[csw] val authConfigKey = "auth-config"
  private[csw] val disabledKey   = "disabled"

  /**
   * Creates an instance of [[csw.aas.core.deployment.AuthConfig]]
   *
   * @param config application config.
   * @param authServiceLocation if authServiceLocation is provided, it will use it and treat it as auth enabled
   *                            otherwise treat it as auth disabled
   */
  def apply(config: Config, authServiceLocation: Option[HttpLocation]): AuthConfig = {
    new AuthConfig(config.getConfig(authConfigKey), authServiceLocation)
  }
}
