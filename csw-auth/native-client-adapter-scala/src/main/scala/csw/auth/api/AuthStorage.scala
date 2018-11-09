package csw.auth.api

import org.keycloak.adapters.KeycloakDeployment
import org.keycloak.representations.{AccessToken, AccessTokenResponse, IDToken}
import csw.auth.Status.Status

trait AuthStorage {

  def saveStatus(status: Status): Unit
  def getStatus: Option[Status]

  def getAccessTokenString: Option[String]
  def getAccessToken: Option[AccessToken]

  def getIdTokenString: Option[String]
  def getIdToken: Option[IDToken]

  def getRefreshTokenString: Option[String]

  def clearStorage(): Unit
  def saveAccessTokenResponse(accessTokenResponse: AccessTokenResponse,
                              keycloakDeployment: KeycloakDeployment): Unit
}
