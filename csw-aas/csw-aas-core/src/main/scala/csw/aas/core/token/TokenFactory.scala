package csw.aas.core.token

import cats.data.EitherT
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

  private[aas] def makeToken(token: String): EitherT[Future, TokenVerificationFailure, AccessToken] =
    if (enablePermissions) rpt.create(token) else tokenVerifier.verifyAndDecode(token)
}
