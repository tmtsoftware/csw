package csw.auth.token
import csw.auth.commons.AuthLogger
import csw.logging.scaladsl.Logger
import org.keycloak.authorization.client.AuthzClient
import pdi.jwt.{JwtJson, JwtOptions}
import play.api.libs.json.Json

import scala.util.Try

private[auth] class RPT(authzClient: AuthzClient) {
  private val log: Logger = AuthLogger.getLogger

  def create(token: String): Try[AccessToken] = {
    log.debug("fetching RPT")
    for {
      rptString   ← Try { authzClient.authorization(token).authorize().getToken }
      claim       ← JwtJson.decode(rptString, JwtOptions(signature = false, expiration = false, notBefore = false))
      accessToken ← Try { Json.parse(claim.toJson).as[AccessToken] }
    } yield accessToken
  }
}

object RPT {
  def apply(authzClient: AuthzClient): RPT = new RPT(authzClient)
}
