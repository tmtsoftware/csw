package csw.auth.core.token
import csw.auth.core.Keycloak.deployment
import csw.auth.core.commons.AuthLogger
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

  private[auth] def makeToken(token: String): Try[AccessToken] = {
    val result = rpt.create(token)
    result match {
      case Failure(e) =>
        error("token string could not be converted to RPT", ex = e)
        Failure(e)
      case x => x
    }
  }
}
