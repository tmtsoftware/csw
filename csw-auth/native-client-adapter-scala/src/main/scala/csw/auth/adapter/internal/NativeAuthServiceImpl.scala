package csw.auth.adapter.internal

import csw.auth.AccessToken
import csw.auth.adapter.api.{AuthStore, NativeAuthService}
import org.keycloak.adapters.KeycloakDeployment
import org.keycloak.adapters.installed.KeycloakInstalled
import org.keycloak.adapters.rotation.AdapterTokenVerifier
import org.keycloak

import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.util.{Failure, Try}

private[auth] class NativeAuthServiceImpl(val keycloakInstalled: KeycloakInstalled, authStore: Option[AuthStore] = None)
    extends NativeAuthService {

  def this(keycloakDeployment: KeycloakDeployment) = this(new KeycloakInstalled(keycloakDeployment))

  def this(keycloakDeployment: KeycloakDeployment, authStore: AuthStore) =
    this(new KeycloakInstalled(keycloakDeployment), Some(authStore))

  def login(): Unit = {
    keycloakInstalled.login()
    updateAuthStore()
  }

  def logout(): Unit = {
    keycloakInstalled.logout()
    clearAuthStore()
  }

  def loginDesktop(): Unit = {
    keycloakInstalled.loginDesktop()
    updateAuthStore()
  }

  def loginManual(): Unit = {
    keycloakInstalled.loginManual()
    updateAuthStore()
  }

  def loginCommandLine(): Boolean = {
    val bool = keycloakInstalled.loginCommandLine()
    if (bool) updateAuthStore()
    bool
  }

  def loginCommandLine(redirectUri: String): Boolean = {
    val bool = keycloakInstalled.loginCommandLine(redirectUri)
    if (bool) updateAuthStore()
    bool
  }

  def getAccessTokenString(minValidity: FiniteDuration = 0.seconds): Try[String] =
    Try {
      getAccessToken(minValidity)
      accessTokenStr().getOrElse(throw new RuntimeException("Access token not found"))
    }

  def getAccessToken(minValidity: FiniteDuration = 0.seconds): Try[AccessToken] =
    accessToken().flatMap { token ⇒
      if (isExpired(token, minValidity)) refreshAccessToken()
      accessTokenStr()
    } match {
      case Some(at) ⇒ AccessToken.verifyAndDecode(at)
      case None     ⇒ Failure(throw new RuntimeException("Access token not found"))
    }

  private def accessToken(): Option[keycloak.representations.AccessToken] = authStore match {
    case Some(store) ⇒
      store.getAccessTokenString.map { at ⇒
        val verifier = AdapterTokenVerifier.createVerifier(
          at,
          keycloakInstalled.getDeployment,
          true,
          classOf[keycloak.representations.AccessToken]
        )
        verifier.getToken
      }
    case None ⇒ Option(keycloakInstalled.getToken)
  }

  private def accessTokenStr() = authStore match {
    case Some(store) ⇒ store.getAccessTokenString
    case None        ⇒ Option(keycloakInstalled.getTokenString)
  }

  private def isExpired(accessToken: keycloak.representations.AccessToken, minValidity: FiniteDuration) = {
    val expires: Long = accessToken.getExpiration.toLong * 1000 - minValidity.toMillis
    expires < System.currentTimeMillis
  }

  private def refreshAccessToken(): Unit = {
    refreshTokenStr().foreach(keycloakInstalled.refreshToken)
    updateAuthStore()
  }

  private def refreshTokenStr() = authStore match {
    case Some(store) ⇒ store.getRefreshTokenString
    case None        ⇒ Option(keycloakInstalled.getRefreshToken)
  }

  private def updateAuthStore(): Unit = {
    val response = keycloakInstalled.getTokenResponse
    authStore.foreach(_.saveTokens(response.getIdToken, response.getToken, response.getRefreshToken))
  }

  private def clearAuthStore(): Unit = authStore.foreach(_.clearStorage())
}
