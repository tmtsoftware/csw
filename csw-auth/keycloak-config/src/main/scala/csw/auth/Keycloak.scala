package csw.auth

import java.io.{ByteArrayInputStream, InputStream}

import com.typesafe.config.{Config, ConfigFactory, ConfigRenderOptions}
import org.keycloak.adapters.{KeycloakDeployment, KeycloakDeploymentBuilder}
import org.keycloak.authorization.client.{AuthzClient, Configuration}

private[auth] object Keycloak {

  //todo: consider removing static state to make this more testable.
  //todo: can we use DI and inject config here?
  private lazy val authConfig: Config  = ConfigFactory.load().getConfig("auth-config")
  private lazy val configJSON: String  = authConfig.root().render(ConfigRenderOptions.concise())
  private def inputStream: InputStream = new ByteArrayInputStream(configJSON.getBytes())

  lazy val deployment: KeycloakDeployment = KeycloakDeploymentBuilder.build(inputStream)

  private lazy val configuration: Configuration = new Configuration(
    deployment.getAuthServerBaseUrl,
    deployment.getRealm,
    deployment.getResourceName,
    deployment.getResourceCredentials,
    deployment.getClient
  )

  lazy val authzClient: AuthzClient = AuthzClient.create(configuration)
}
