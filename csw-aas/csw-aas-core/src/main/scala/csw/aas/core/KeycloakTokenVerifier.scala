package csw.aas.core
import org.keycloak.adapters.KeycloakDeployment
import org.keycloak.adapters.rotation.AdapterTokenVerifier
import org.keycloak.representations.{AccessToken => KeycloakAccessToken}

import scala.concurrent.{blocking, ExecutionContext, Future}

private[aas] class KeycloakTokenVerifier {

  /**
   * Verifies given access token string using keycloak library.
   * @param token access token string
   * @return Verified keycloak access token
   */
  def verifyToken(
      token: String,
      keycloakDeployment: KeycloakDeployment
  )(implicit ec: ExecutionContext): Future[KeycloakAccessToken] = Future {
    blocking { AdapterTokenVerifier.verifyToken(token, keycloakDeployment) }
  }
}
