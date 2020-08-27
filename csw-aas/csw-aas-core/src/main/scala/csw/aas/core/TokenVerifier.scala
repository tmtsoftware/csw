package csw.aas.core

import csw.aas.core.TokenVerificationFailure.{InvalidToken, TokenExpired}
import csw.aas.core.commons.AuthLogger
import csw.aas.core.deployment.AuthConfig
import csw.aas.core.token.AccessToken
import io.bullet.borer.Json
import org.keycloak.common.VerificationException
import org.keycloak.exceptions.TokenNotActiveException
import org.keycloak.representations.{AccessToken => KAccessToken}
import pdi.jwt.{Jwt, JwtOptions}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NonFatal

/**
 * Verifies & decodes the access token into [[csw.aas.core.token.AccessToken]]
 */
class TokenVerifier private[aas] (keycloakTokenVerifier: KeycloakTokenVerifier, authConfig: AuthConfig) extends AuthCodecs {

  private val logger = AuthLogger.getLogger

  private val keycloakDeployment = authConfig.getDeployment

  private def verify(token: String)(implicit ec: ExecutionContext): Future[Either[TokenVerificationFailure, KAccessToken]] =
    keycloakTokenVerifier
      .verifyToken(token, keycloakDeployment)
      .map(Right(_))
      .recover {
        case _: TokenNotActiveException =>
          logger.warn(s"token is expired")
          Left(TokenExpired)
        case ex: VerificationException =>
          logger.error("token verification failed", ex = ex)
          Left(InvalidToken(ex.getMessage))
      }

  private def decode(token: String): Either[TokenVerificationFailure, AccessToken] =
    Try {
      val claim = Jwt.decodeRaw(token, JwtOptions(signature = false, expiration = false, notBefore = false)).get
      Json.decode(claim.getBytes()).to[AccessToken].value.copy(value = token)
    }.toEither.left.map {
      case NonFatal(e) =>
        logger.error("token verification failed", ex = e)
        InvalidToken(e.getMessage)
    }

  /**
   * Verifies the access token string for signature and expiry date
   * and then decodes it into [[csw.aas.core.token.AccessToken]]
   *
   * @param token access token string
   */
  def verifyAndDecode(token: String)(implicit ec: ExecutionContext): Future[Either[TokenVerificationFailure, AccessToken]] = {
    verify(token).map(eVer => {
      eVer.flatMap(_ => decode(token))
    })
  }
}
object TokenVerifier {
  def apply(authConfig: AuthConfig): TokenVerifier = new TokenVerifier(new KeycloakTokenVerifier, authConfig)
}
