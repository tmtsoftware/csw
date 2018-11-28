package csw.auth.core.token
import csw.auth.core.commons.AuthLogger
import csw.logging.scaladsl.Logger
import org.keycloak.authorization.client.AuthzClient
import pdi.jwt.{JwtJson, JwtOptions}

import scala.util.Try

private[auth] class RPT(authzClient: AuthzClient) {
  private val log: Logger = AuthLogger.getLogger

  def create(token: String): Try[AccessToken] = {
    log.debug("fetching RPT")
    for {
      rptString ← Try { authzClient.authorization(token).authorize().getToken }
      accessToken ← JwtJson
        .decodeJson(token, JwtOptions(signature = false, expiration = false, notBefore = false))
        .map(_.as[AccessToken])
    } yield accessToken
  }
}

object RPT {
  def apply(authzClient: AuthzClient): RPT = new RPT(authzClient)
}
