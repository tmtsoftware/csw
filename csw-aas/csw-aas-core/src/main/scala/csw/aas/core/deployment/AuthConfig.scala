package csw.aas.core.deployment
import java.io.{ByteArrayInputStream, InputStream}

import com.typesafe.config._
import csw.aas.core.commons.AuthLogger
import csw.aas.core.deployment.AuthConfig._
import csw.location.api.models.HttpLocation
import org.keycloak.adapters.{KeycloakDeployment, KeycloakDeploymentBuilder}
import org.keycloak.authorization.client.Configuration

import scala.language.implicitConversions
import scala.util.Try

/**
 * Represents Authorization configuration
 *
 * @param config application config
 * @param authServiceLocation if authServiceLocation is provided, it will use it,
 *                            otherwise it will rely on config for auth-service-url
 */
class AuthConfig private (config: Config, authServiceLocation: Option[HttpLocation], disabledMaybe: Option[Boolean]) {

  private val enablePermissionsKey = "enable-permissions"
  private val clientIdKey          = "client-id"

  val permissionsEnabled: Boolean = optionalBoolean(config, enablePermissionsKey)
  val disabled: Boolean           = disabledMaybe.getOrElse(optionalBoolean(config, disabledKey))
  private val logger              = AuthLogger.getLogger

  import logger._

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
        debug("creating keycloak deployment for disabled configuration")
        val configWithDisabledUrl =
          config
            .withValue("auth-server-url", ConfigValueFactory.fromAnyRef("http://disabled-auth-service"))
            .withValue(clientIdKey, ConfigValueFactory.fromAnyRef("security-disabled-client"))

        convertToDeployment(configWithDisabledUrl)
      case None =>
        debug("creating keycloak deployment with configured keycloak location")
        convertToDeployment(config)
      case Some(location) =>
        debug("creating keycloak deployment with resolved keycloak location")
        val configWithResolvedAuthUrl = config.withValue("auth-server-url", ConfigValueFactory.fromAnyRef(location.uri.toString))
        convertToDeployment(configWithResolvedAuthUrl)
    }

  private def convertToDeployment(config: Config): KeycloakDeployment = {
    val clientId = config.getString(clientIdKey)
    val safeConfig = config
      .withoutPath(enablePermissionsKey)
      .withoutPath(disabledKey)
      .withoutPath(clientIdKey)
      .withValue("resource", ConfigValueFactory.fromAnyRef(clientId))

    debug("converting auth config to json")

    val configJSON: String = safeConfig.root().render(ConfigRenderOptions.concise())

    val inputStream: InputStream = new ByteArrayInputStream(configJSON.getBytes())

    debug("initializing keycloak deployment instance from config")
    val deployment: KeycloakDeployment = KeycloakDeploymentBuilder.build(inputStream)

    info("keycloak deployment created successfully with keycloak configurations")
    deployment
  }
}

object AuthConfig {

  private val logger = AuthLogger.getLogger
  import logger._

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
    debug("loading auth config")
    new AuthConfig(config.getConfig(authConfigKey), authServerLocation, disabledMaybe)
  }

  private def optionalBoolean(config: Config, key: String) = {
    val mayBeValue = Try { config.getBoolean(key) }.toOption
    mayBeValue.nonEmpty && mayBeValue.get
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
