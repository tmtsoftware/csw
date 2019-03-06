package csw.aas.core.token

import csw.aas.core.deployment.AuthConfig._
import csw.aas.core.{TokenVerificationFailure, TokenVerifier}
import org.keycloak.adapters.KeycloakDeployment
import org.keycloak.authorization.client.AuthzClient

import scala.concurrent.{ExecutionContext, Future}

class TokenFactory(keycloakDeployment: KeycloakDeployment, tokenVerifier: TokenVerifier, enablePermissions: Boolean)(
    implicit ec: ExecutionContext
) {
  private lazy val authzClient: AuthzClient = AuthzClient.create(keycloakDeployment)

  private lazy val rpt: RPT = RPT(authzClient)

  /**
   * If "enable-permissions" is set to true, it will fetch a new token from AAS server.
   * This token will contain all the token information along with permissions. If "enable-permissions" is set to false,
   * it will validate the token string for signature and expiry and then decode it into
   * [[csw.aas.core.token.AccessToken]]
   * @param token Access token string
   */
  private[aas] def makeToken(token: String): Future[Either[TokenVerificationFailure, AccessToken]] =
    if (enablePermissions) rpt.create(token) else tokenVerifier.verifyAndDecode(token)
}
