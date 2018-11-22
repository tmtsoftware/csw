package csw.auth.core

import csw.auth.core.TokenVerificationFailure.{InvalidToken, TokenExpired}
import csw.auth.core.token.AccessToken
import org.keycloak.adapters.KeycloakDeployment
import org.keycloak.adapters.rotation.AdapterTokenVerifier
import org.keycloak.common.VerificationException
import org.keycloak.exceptions.TokenNotActiveException
import org.keycloak.representations.{AccessToken ⇒ KeycloakAccessToken}
import pdi.jwt.{JwtJson, JwtOptions}

import scala.util.Try

private[auth] class KeycloakTokenVerifier {
  def verifyToken(token: String, keycloakDeployment: KeycloakDeployment): Try[KeycloakAccessToken] =
    Try { AdapterTokenVerifier.verifyToken(token, Keycloak.deployment) }
}

class TokenVerifier private[auth] (keycloakTokenVerifier: KeycloakTokenVerifier) {

  def verifyAndDecode(token: String): Either[TokenVerificationFailure, AccessToken] = {

    val keycloakToken = keycloakTokenVerifier.verifyToken(token, Keycloak.deployment).toEither.left.flatMap {
      case _: TokenNotActiveException => Left(TokenExpired)
      case ex: VerificationException  => Left(InvalidToken(ex.getMessage))
    }

    keycloakToken.flatMap { _ =>
      JwtJson
        .decodeJson(token, JwtOptions(signature = false, expiration = false, notBefore = false))
        .map(_.as[AccessToken])
        .toEither
        .left
        .flatMap { e: Throwable => // todo: shouldn't we just catch NonFatal errors and rethrow Fatal?
          Left(InvalidToken(e.getMessage))
        }
    }
  }
}

object TokenVerifier {
  def apply(): TokenVerifier = new TokenVerifier(new KeycloakTokenVerifier)
}
