package csw.aas.core.token

import csw.aas.core.commons.AuthLogger
import csw.aas.core.deployment.AuthConfig
import csw.aas.core.deployment.AuthConfig._
import org.keycloak.adapters.KeycloakDeployment
import org.keycloak.authorization.client.AuthzClient

import scala.util.{Failure, Success, Try}

class TokenFactory(keycloakDeployment: KeycloakDeployment) {

  private val logger = AuthLogger.getLogger
  import logger._

  private lazy val authzClient: AuthzClient = AuthzClient.create(keycloakDeployment)

  private lazy val rpt: RPT = RPT(authzClient)

  private[aas] def makeToken(token: String): Try[AccessToken] = {
    val result = rpt.create(token)
    result match {
      case Failure(e) =>
        error("token string could not be converted to RPT", ex = e)
        Failure(e)
      case x @ Success(value) => {
        debug(s"authentication succeeded for ${value.userOrClientName}")
        x
      }
    }
  }
}
