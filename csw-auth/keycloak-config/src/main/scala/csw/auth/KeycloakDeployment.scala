package csw.auth
import java.io.{ByteArrayInputStream, InputStream}

import com.typesafe.config.{Config, ConfigFactory, ConfigRenderOptions}
import org.keycloak.adapters.{KeycloakDeployment, KeycloakDeploymentBuilder}

private[auth] object KeycloakDeployment {

  val instance: KeycloakDeployment = {

    val authConfig: Config = ConfigFactory
      .load()
      .getConfig("auth-config")

    val configJSON: String =
      authConfig.root().render(ConfigRenderOptions.concise())

    val inputStream: InputStream = new ByteArrayInputStream(configJSON.getBytes())

    KeycloakDeploymentBuilder.build(inputStream)
  }
}
