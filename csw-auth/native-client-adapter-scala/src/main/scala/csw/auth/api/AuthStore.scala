package csw.auth.api

import csw.auth.AccessToken
import org.keycloak.adapters.KeycloakDeployment
import org.keycloak.representations.{AccessTokenResponse, IDToken}

trait AuthStore {

  def getAccessTokenString: Option[String]
  def getAccessToken(kd: KeycloakDeployment): Option[AccessToken]

  def getIdTokenString: Option[String]
  def getIdToken: Option[IDToken]

  def getRefreshTokenString: Option[String]

  def clearStorage(): Unit

  //FIXME: do we really need kd here
  def saveAccessTokenResponse(accessTokenResponse: AccessTokenResponse, keycloakDeployment: KeycloakDeployment): Unit
}
