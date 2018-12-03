package csw.auth.core.token
import csw.auth.core.commons.AuthLogger
import org.keycloak.authorization.client.AuthzClient
import pdi.jwt.{JwtJson, JwtOptions}

import scala.util.{Failure, Success, Try}

private[auth] class RPT(authzClient: AuthzClient) {
  private val logger = AuthLogger.getLogger
  import logger._

  def create(token: String): Try[AccessToken] = {
    debug("fetching RPT from keycloak")
    val result = for {
      rptString ← Try { authzClient.authorization(token).authorize().getToken }
      accessToken ← JwtJson
        .decodeJson(rptString, JwtOptions(signature = false, expiration = false, notBefore = false))
        .map(_.as[AccessToken])
    } yield accessToken

    result match {
      case Success(value) => { debug("successfully fetched RPT from keycloak"); Success(value) }
      case Failure(e)     => { error("error while fetching RPT from keycloak", ex = e); Failure(e) }
    }
  }
}

object RPT {
  def apply(authzClient: AuthzClient): RPT = new RPT(authzClient)
}
