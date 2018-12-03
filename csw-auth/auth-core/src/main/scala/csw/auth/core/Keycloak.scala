package csw.auth.core
import java.io.{ByteArrayInputStream, InputStream}

import com.typesafe.config.{Config, ConfigFactory, ConfigRenderOptions}
import csw.auth.core.commons.AuthLogger
import org.keycloak.adapters.{KeycloakDeployment, KeycloakDeploymentBuilder}

private[auth] object Keycloak {

  //todo: consider removing static state to make this more testable.
  //todo: can we use DI and inject config here?

  private val logger = AuthLogger.getLogger
  import logger._

  debug("loading auth-config")
  private val authConfig: Config = ConfigFactory.load().getConfig("auth-config")
  debug("converting hocon auth-config to json format")
  private val configJSON: String       = authConfig.root().render(ConfigRenderOptions.concise())
  private val inputStream: InputStream = new ByteArrayInputStream(configJSON.getBytes())

  debug("initializing keycloak deployment instance from config")
  val deployment: KeycloakDeployment = KeycloakDeploymentBuilder.build(inputStream)
  info("keycloak deployment created successfully with keycloak configurations")
}
