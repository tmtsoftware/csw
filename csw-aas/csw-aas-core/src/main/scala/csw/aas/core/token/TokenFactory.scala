package csw.aas.core.token

import csw.aas.core.commons.AuthLogger
import csw.aas.core.deployment.Keycloak.deployment
import org.keycloak.authorization.client.{AuthzClient, Configuration}

import scala.util.{Failure, Try}

class TokenFactory {
  private lazy val configuration: Configuration = new Configuration(
    deployment.getAuthServerBaseUrl,
    deployment.getRealm,
    deployment.getResourceName,
    deployment.getResourceCredentials,
    deployment.getClient
  )

  private val logger = AuthLogger.getLogger
  import logger._

  private lazy val authzClient: AuthzClient = AuthzClient.create(configuration)

  private lazy val rpt: RPT = RPT(authzClient)

  private[aas] def makeToken(token: String): Try[AccessToken] = {
    val result = rpt.create(token)
    result match {
      case Failure(e) =>
        error("token string could not be converted to RPT", ex = e)
        Failure(e)
      case x => x
    }
  }
}
