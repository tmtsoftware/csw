package csw.auth.adapter.api

import org.keycloak.adapters.KeycloakDeployment
import org.keycloak.representations.{AccessToken, AccessTokenResponse}

trait AuthStore {

  def getAccessTokenString: Option[String]

  //FIXME: Delete me
  private[auth] def getAccessToken(kd: KeycloakDeployment): Option[AccessToken]

  def getIdTokenString: Option[String]

  def getRefreshTokenString: Option[String]

  def clearStorage(): Unit

  //FIXME: do we really need kd here
  def saveAccessTokenResponse(accessTokenResponse: AccessTokenResponse, keycloakDeployment: KeycloakDeployment): Unit
}
