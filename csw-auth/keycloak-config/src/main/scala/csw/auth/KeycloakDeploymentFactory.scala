package csw.auth
import java.io.{ByteArrayInputStream, InputStream}

import com.typesafe.config.{ConfigFactory, ConfigRenderOptions}
import org.keycloak.adapters.{KeycloakDeployment, KeycloakDeploymentBuilder}

private[auth] object KeycloakDeploymentFactory {

  private val authConfig = ConfigFactory
    .load()
    .getConfig("auth-config")

  def createInstance(): KeycloakDeployment = {

    val configJSON: String =
      authConfig.root().render(ConfigRenderOptions.concise())

    val inputStream: InputStream = new ByteArrayInputStream(configJSON.getBytes())

    KeycloakDeploymentBuilder.build(inputStream)
  }
}
