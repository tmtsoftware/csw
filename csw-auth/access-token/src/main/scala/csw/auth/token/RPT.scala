package csw.auth.token
import csw.auth.Keycloak.deployment
import csw.auth.commons.AuthLogger
import csw.logging.scaladsl.Logger
import org.keycloak.authorization.client.{AuthzClient, Configuration}
import pdi.jwt.{JwtJson, JwtOptions}
import play.api.libs.json.Json

import scala.util.Try

private[auth] object RPT {
  private val log: Logger = AuthLogger.getLogger

  private val configuration: Configuration = new Configuration(
    deployment.getAuthServerBaseUrl,
    deployment.getRealm,
    deployment.getResourceName,
    deployment.getResourceCredentials,
    deployment.getClient
  )
  private val authzClient: AuthzClient = AuthzClient.create(configuration)

  def create(token: String): Try[AccessToken] = {
    log.debug("fetching RPT")
    for {
      rptString   ← Try { authzClient.authorization(token).authorize().getToken }
      claim       ← JwtJson.decode(rptString, JwtOptions(signature = false, expiration = false, notBefore = false))
      accessToken ← Try { Json.parse(claim.toJson).as[AccessToken] }
    } yield accessToken
  }
}
