package csw.aas.core

import csw.aas.core.TokenVerificationFailure.{InvalidToken, TokenExpired}
import csw.aas.core.commons.AuthLogger
import csw.aas.core.deployment.AuthConfig
import csw.aas.core.token.AccessToken
import org.keycloak.adapters.KeycloakDeployment
import org.keycloak.adapters.rotation.AdapterTokenVerifier
import org.keycloak.common.VerificationException
import org.keycloak.exceptions.TokenNotActiveException
import org.keycloak.representations.{AccessToken => KeycloakAccessToken}
import pdi.jwt.{JwtJson, JwtOptions}

import scala.util.Try

private[aas] class KeycloakTokenVerifier {
  def verifyToken(token: String, keycloakDeployment: KeycloakDeployment): Try[KeycloakAccessToken] =
    Try { AdapterTokenVerifier.verifyToken(token, keycloakDeployment) }
}

class TokenVerifier private[aas] (keycloakTokenVerifier: KeycloakTokenVerifier, authConfig: AuthConfig) {

  private val logger = AuthLogger.getLogger
  import logger._

  private val keycloakDeployment = authConfig.getDeployment

  def verifyAndDecode(token: String): Either[TokenVerificationFailure, AccessToken] = {
    val keycloakToken: Either[TokenVerificationFailure, KeycloakAccessToken] =
      keycloakTokenVerifier.verifyToken(token, keycloakDeployment).toEither.left.flatMap {
        case _: TokenNotActiveException => {
          warn(s"token is expired")
          Left(TokenExpired)
        }
        case ex: VerificationException => {
          error("token verification failed", ex = ex)
          Left(InvalidToken(ex.getMessage))
        }
      }

    val result = keycloakToken.flatMap { _ =>
      JwtJson
        .decodeJson(token, JwtOptions(signature = false, expiration = false, notBefore = false))
        .map(_.as[AccessToken])
        .toEither
        .left
        .flatMap { e: Throwable => // todo: shouldn't we just catch NonFatal errors and rethrow Fatal?
          Left(InvalidToken(e.getMessage))
        }
    }

    result match {
      case Left(e) => error("token verification failed", Map("error" -> e))
      case x       => x
    }

    result
  }
}

object TokenVerifier {
  def apply(authConfig: AuthConfig): TokenVerifier =
    new TokenVerifier(new KeycloakTokenVerifier, authConfig)
}
