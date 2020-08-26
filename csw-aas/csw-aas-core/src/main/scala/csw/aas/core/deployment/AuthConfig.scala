package csw.aas.core.deployment
import java.io.{ByteArrayInputStream, InputStream}

import com.typesafe.config._
import csw.aas.core.commons.AuthLogger
import csw.aas.core.deployment.AuthConfig._
import csw.aas.core.utils.ConfigExt._
import csw.location.api.models.HttpLocation
import org.keycloak.adapters.{KeycloakDeployment, KeycloakDeploymentBuilder}
import org.keycloak.authorization.client.Configuration

import scala.language.implicitConversions

/**
 * Represents Authorization configuration
 *
 * @param config application config
 * @param authServiceLocation if authServiceLocation is provided, it will use it,
 *                            otherwise it will rely on config for auth-service-url
 */
class AuthConfig private (config: Config, authServiceLocation: Option[HttpLocation], disabledMaybe: Option[Boolean]) {

  private val clientIdKey = "client-id"
  val disabled: Boolean   = disabledMaybe.getOrElse(config.getBooleanOrFalse(disabledKey))
  private val logger      = AuthLogger.getLogger

  /**
   * Creates an instance of KeycloakDeployment using app config.
   * If authServiceLocation has a value, it will use it,
   * otherwise it will rely on config for auth-service-url
   *
   * @return
   */
  private[csw] def getDeployment: KeycloakDeployment =
    authServiceLocation match {
      case None if disabled =>
        logger.debug("creating keycloak deployment for disabled configuration")
        val configForDisabledAuth =
          config
            .withValue("auth-server-url", ConfigValueFactory.fromAnyRef("http://disabled-auth-service"))
            .withValue(clientIdKey, ConfigValueFactory.fromAnyRef("security-disabled-client"))

        convertToDeployment(configForDisabledAuth)
      case None =>
        logger.debug("creating keycloak deployment with pre-configured keycloak location")
        convertToDeployment(config)
      case Some(location) =>
        logger.debug("creating keycloak deployment with keycloak location from location service")
        val configWithResolvedAuthUrl = config.withValue("auth-server-url", ConfigValueFactory.fromAnyRef(location.uri.toString))
        convertToDeployment(configWithResolvedAuthUrl)
    }

  private def convertToDeployment(config: Config): KeycloakDeployment = {
    val clientId = config.getString(clientIdKey)
    val safeConfig = config
      .withoutPath(disabledKey)
      .withoutPath(clientIdKey)
      .withValue("resource", ConfigValueFactory.fromAnyRef(clientId))

    logger.debug("converting auth config to json")

    val configJSON: String = safeConfig.root().render(ConfigRenderOptions.concise())

    val inputStream: InputStream = new ByteArrayInputStream(configJSON.getBytes())

    logger.debug("initializing keycloak deployment instance from config")
    val deployment: KeycloakDeployment = KeycloakDeploymentBuilder.build(inputStream)

    logger.info("keycloak deployment created successfully with keycloak configurations")
    deployment
  }
}

object AuthConfig {

  private val logger = AuthLogger.getLogger

  private[csw] val authConfigKey = "auth-config"
  private[csw] val disabledKey   = "disabled"

  /**
   * Creates an instance of [[csw.aas.core.deployment.AuthConfig]]
   *
   * @param config application config. If not provided, it will load the config automatically
   * @param authServerLocation if authServerLocation is provided, it will use it,
   *                            otherwise it will rely on config for auth-service-url
   * @param disabledMaybe if provided, will ignore `disabled` key in configuration
   */
  def create(
      config: Config = ConfigFactory.load(),
      authServerLocation: Option[HttpLocation] = None,
      disabledMaybe: Option[Boolean] = None
  ): AuthConfig = {
    logger.debug("loading auth config")
    new AuthConfig(config.getConfig(authConfigKey), authServerLocation, disabledMaybe)
  }

  private[aas] implicit def deploymentToConfig(deployment: KeycloakDeployment): Configuration =
    new Configuration(
      deployment.getAuthServerBaseUrl,
      deployment.getRealm,
      deployment.getResourceName,
      deployment.getResourceCredentials,
      deployment.getClient
    )

}
