package csw.aas.core.deployment
import java.io.{ByteArrayInputStream, InputStream}

import com.typesafe.config._
import csw.aas.core.commons.AuthLogger
import csw.location.api.models.HttpLocation
import org.keycloak.adapters.{KeycloakDeployment, KeycloakDeploymentBuilder}
import org.keycloak.authorization.client.Configuration

import scala.language.implicitConversions

class AuthConfig private (config: Config, authServiceLocation: Option[HttpLocation]) {

  private val logger = AuthLogger.getLogger
  import logger._

  private[csw] def getDeployment: KeycloakDeployment =
    authServiceLocation match {
      case None => {
        debug("creating keycloak deployment with configured keycloak location")
        convertToDeployment(config)
      }
      case Some(location) =>
        debug("creating keycloak deployment with resolved keycloak location")
        val configWithResolvedAuthUrl = config.withValue("auth-server-url", ConfigValueFactory.fromAnyRef(location.uri.toString))
        convertToDeployment(configWithResolvedAuthUrl)
    }

  private def convertToDeployment(config: Config): KeycloakDeployment = {
    debug("converting auth config to json")
    val configJSON: String = config.root().render(ConfigRenderOptions.concise())

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

  def loadFromAppConfig: AuthConfig = {
    debug("loading auth config")
    val config = ConfigFactory.load().getConfig("auth-config")
    new AuthConfig(config, None)
  }

  def loadFromAppConfig(authServerLocation: HttpLocation): AuthConfig = {
    debug("loading auth config")
    val config = ConfigFactory.load().getConfig("auth-config")
    new AuthConfig(config, Some(authServerLocation))
  }

  private[aas] implicit def deploymentToConfig(deployment: KeycloakDeployment): Configuration = {
    new Configuration(
      deployment.getAuthServerBaseUrl,
      deployment.getRealm,
      deployment.getResourceName,
      deployment.getResourceCredentials,
      deployment.getClient
    )
  }
}
