package csw.auth.core.token
import csw.auth.core.Keycloak.deployment
import org.keycloak.authorization.client.{AuthzClient, Configuration}

import scala.util.Try

class TokenFactory {
  private lazy val configuration: Configuration = new Configuration(
    deployment.getAuthServerBaseUrl,
    deployment.getRealm,
    deployment.getResourceName,
    deployment.getResourceCredentials,
    deployment.getClient
  )
  private lazy val authzClient: AuthzClient = AuthzClient.create(configuration)

  private lazy val rpt: RPT = RPT(authzClient)

  private[auth] def makeToken(token: String): Try[AccessToken] = {
    rpt.create(token)
  }
}
