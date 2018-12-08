package csw.aas.core

import cats.data._
import cats.implicits._
import csw.aas.core.TokenVerificationFailure.{InvalidToken, TokenExpired}
import csw.aas.core.commons.AuthLogger
import csw.aas.core.deployment.AuthConfig
import csw.aas.core.token.AccessToken
import org.keycloak.common.VerificationException
import org.keycloak.exceptions.TokenNotActiveException
import org.keycloak.representations.{AccessToken => KAccessToken}
import pdi.jwt.{JwtJson, JwtOptions}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class TokenVerifier private[aas] (keycloakTokenVerifier: KeycloakTokenVerifier, authConfig: AuthConfig) {

  private val logger = AuthLogger.getLogger
  import logger._

  private val keycloakDeployment = authConfig.getDeployment

  private def verify(token: String)(implicit ec: ExecutionContext): EitherT[Future, TokenVerificationFailure, KAccessToken] =
    keycloakTokenVerifier
      .verifyToken(token, keycloakDeployment)
      .leftMap {
        case _: TokenNotActiveException =>
          warn(s"token is expired")
          TokenExpired
        case ex: VerificationException =>
          error("token verification failed", ex = ex)
          InvalidToken(ex.getMessage)
      }

  private def decode(token: String): Either[TokenVerificationFailure, AccessToken] =
    JwtJson
      .decodeJson(token, JwtOptions(signature = false, expiration = false, notBefore = false))
      .map(_.as[AccessToken])
      .toEither
      .left
      .map {
        case NonFatal(e) =>
          error("token verification failed", Map("error" -> e))
          InvalidToken(e.getMessage)
      }

  def verifyAndDecode(token: String)(implicit ec: ExecutionContext): EitherT[Future, TokenVerificationFailure, AccessToken] =
    for {
      _  ← verify(token)
      at ← EitherT.fromEither[Future](decode(token))
    } yield at
}

object TokenVerifier {
  def apply(authConfig: AuthConfig): TokenVerifier = new TokenVerifier(new KeycloakTokenVerifier, authConfig)
}
