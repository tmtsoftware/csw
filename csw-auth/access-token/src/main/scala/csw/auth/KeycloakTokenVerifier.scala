package csw.auth
import csw.auth.TokenVerificationFailure.{InvalidToken, TokenExpired}
import csw.auth.token.AccessToken
import org.keycloak.adapters.KeycloakDeployment
import org.keycloak.adapters.rotation.AdapterTokenVerifier
import org.keycloak.common.VerificationException
import org.keycloak.exceptions.TokenNotActiveException
import org.keycloak.representations.{AccessToken => KeycloakAccessToken}
import pdi.jwt.{JwtJson, JwtOptions}

private[auth] class KeycloakTokenVerifier {
  def verifyToken(token: String, keycloakDeployment: KeycloakDeployment): KeycloakAccessToken =
    AdapterTokenVerifier.verifyToken(token, Keycloak.deployment)
}

class TokenVerifier private[auth] (keycloakTokenVerifier: KeycloakTokenVerifier) {

  def verifyAndDecode(token: String): Either[TokenVerificationFailure, AccessToken] = {

    val keycloakToken: Either[TokenVerificationFailure, KeycloakAccessToken] = try {
      Right(keycloakTokenVerifier.verifyToken(token, Keycloak.deployment))
    } catch {
      case ex: TokenNotActiveException =>
        Left(TokenExpired)
      case ex: VerificationException =>
        Left(InvalidToken(ex.getMessage))
    }

    keycloakToken.flatMap(_ => {
      JwtJson
        .decodeJson(token, JwtOptions(signature = false, expiration = false, notBefore = false))
        .map(_.as[AccessToken])
        .toEither
        .left
        .flatMap { e: Throwable =>
          Left(InvalidToken(e.getMessage))
        }
    })
  }
}

object TokenVerifier {
  def apply(): TokenVerifier = new TokenVerifier(new KeycloakTokenVerifier)
}
