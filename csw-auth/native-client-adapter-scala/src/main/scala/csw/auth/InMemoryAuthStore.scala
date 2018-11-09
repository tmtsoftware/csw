package csw.auth

import csw.auth.Status.Status
import csw.auth.api.AuthStorage
import org.keycloak.adapters.KeycloakDeployment
import org.keycloak.adapters.rotation.AdapterTokenVerifier
import org.keycloak.representations.{AccessToken, AccessTokenResponse, IDToken}

private[auth] object InMemoryAuthStore extends AuthStorage {

  private var accessTokenResponse: Option[AccessTokenResponse] = None
  private var accessTokenObject: Option[AccessToken] = None
  private var idTokenObject: Option[IDToken] = None
  private var status: Option[Status] = None

  override def saveStatus(status: Status): Unit = this.status = Some(status)

  override def getAccessTokenString: Option[String] =
    this.accessTokenResponse match {
      case Some(at) => Some(at.getToken)
      case _        => None
    }

  override def getAccessToken: Option[AccessToken] = accessTokenObject

  override def getIdTokenString: Option[String] = accessTokenResponse match {
    case Some(at) => Some(at.getIdToken)
    case _        => None
  }

  override def getIdToken: Option[IDToken] = idTokenObject

  override def getRefreshTokenString: Option[String] =
    accessTokenResponse match {
      case Some(at) => Some(at.getRefreshToken)
      case _        => None
    }

  override def getStatus: Option[Status] = this.status

  override def clearStorage(): Unit = {
    accessTokenResponse = None
    accessTokenObject = None
    idTokenObject = None
    status = None
  }

  //todo: we need to somehow stop using Keycloak's access token class and use ours
  override def saveAccessTokenResponse(
      accessTokenResponse: AccessTokenResponse,
      keycloakDeployment: KeycloakDeployment): Unit = {
    this.accessTokenResponse = Some(accessTokenResponse)

    //todo:where does the public key come from?
    //todo: remove the verification. we only need decoding here
    val tokens = AdapterTokenVerifier.verifyTokens(getAccessTokenString.get,
                                                   getIdTokenString.get,
                                                   keycloakDeployment)
    this.accessTokenObject = Some(tokens.getAccessToken)
    this.idTokenObject = Some(tokens.getIdToken)
  }
}
