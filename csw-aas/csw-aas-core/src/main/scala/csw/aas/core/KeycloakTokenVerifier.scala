package csw.aas.core
import cats.data.EitherT
import org.keycloak.adapters.KeycloakDeployment
import org.keycloak.adapters.rotation.AdapterTokenVerifier
import org.keycloak.representations.{AccessToken ⇒ KeycloakAccessToken}

import scala.concurrent.{blocking, ExecutionContext, Future}
import scala.util.Try

private[aas] class KeycloakTokenVerifier {

  def verifyToken(
      token: String,
      keycloakDeployment: KeycloakDeployment
  )(implicit ec: ExecutionContext): EitherT[Future, Throwable, KeycloakAccessToken] = EitherT {
    Future {
      blocking {
        Try(AdapterTokenVerifier.verifyToken(token, keycloakDeployment)).toEither
      }
    }
  }
}
