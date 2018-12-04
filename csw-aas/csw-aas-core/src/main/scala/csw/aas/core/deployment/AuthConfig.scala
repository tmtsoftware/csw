package csw.aas.core.deployment
import java.io.{ByteArrayInputStream, InputStream}

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import com.typesafe.config._
import csw.aas.core.commons.AuthLogger
import csw.aas.core.deployment.AuthConfig.AuthServiceLocation
import csw.location.api.models.Location
import org.keycloak.adapters.{KeycloakDeployment, KeycloakDeploymentBuilder}
import org.keycloak.authorization.client.Configuration

import scala.language.implicitConversions

class AuthConfig private (config: Config, authServiceLocation: Option[AuthServiceLocation]) {

  private val logger = AuthLogger.getLogger
  import logger._

  def this(authServerLocation: Option[AuthServiceLocation] = None) =
    this(ConfigFactory.load().getConfig("auth-config"), authServerLocation)

  private[csw] def getDeployment: KeycloakDeployment =
    authServiceLocation match {
      case None => convertToDeployment(config)
      case Some(location) =>
        debug("resolving keycloak server")
        val uri: Uri     = Uri(location.uri.toString)
        val fullUrl: Uri = uri.withPath(Path / "auth")
        val newConfig    = config.withValue("auth-server-url", ConfigValueFactory.fromAnyRef(fullUrl.toString()))
        convertToDeployment(newConfig)
    }

  private def convertToDeployment(config: Config): KeycloakDeployment = {
    debug("converting config to json")
    val configJSON: String = config.root().render(ConfigRenderOptions.concise())

    val inputStream: InputStream = new ByteArrayInputStream(configJSON.getBytes())

    debug("initializing keycloak deployment instance from config")
    val deployment: KeycloakDeployment = KeycloakDeploymentBuilder.build(inputStream)

    info("keycloak deployment created successfully with keycloak configurations")
    deployment
  }
}

object AuthConfig {

  type AuthServiceLocation = Location

  def loadFromAppConfig: AuthConfig = new AuthConfig()

  def fromConfig(config: Config): AuthConfig = new AuthConfig(config, None)

  def resolveFromLocationServer(authServerLocation: AuthServiceLocation): AuthConfig =
    new AuthConfig(Some(authServerLocation))

  def resolveFromLocationServer(config: Config, authServerLocation: AuthServiceLocation): AuthConfig =
    new AuthConfig(config, Some(authServerLocation))

  implicit def deploymentToConfig(deployment: KeycloakDeployment): Configuration = {
    new Configuration(
      deployment.getAuthServerBaseUrl,
      deployment.getRealm,
      deployment.getResourceName,
      deployment.getResourceCredentials,
      deployment.getClient
    )
  }
}
