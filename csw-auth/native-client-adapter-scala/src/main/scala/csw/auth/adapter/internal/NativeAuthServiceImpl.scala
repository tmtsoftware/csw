package csw.auth.adapter.internal

import csw.auth.adapter.api.{AuthStore, NativeAuthService}
import csw.auth.{AccessToken, TokenExpired, TokenFailure, TokenMissing}
import org.keycloak.adapters.KeycloakDeployment
import org.keycloak.adapters.installed.KeycloakInstalled

import scala.concurrent.duration.{DurationLong, FiniteDuration}

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

  def getAccessTokenString(minValidity: FiniteDuration = 0.seconds): Option[String] = {
    getAccessToken(minValidity)
    accessTokenStr()
  }

  def getAccessToken(minValidity: FiniteDuration = 0.seconds): Option[AccessToken] = {
    def getNewToken = {
      refreshAccessToken()
      accessTokenStr().flatMap(AccessToken.verifyAndDecode(_).toOption)
    }

    accessToken() match {
      case Right(at)          ⇒ if (isExpired(at, minValidity)) getNewToken else Some(at)
      case Left(TokenExpired) ⇒ getNewToken
      case _                  ⇒ None
    }
  }

  private def accessToken(): Either[TokenFailure, AccessToken] = authStore match {
    case Some(store) ⇒
      store.getAccessTokenString match {
        case Some(at) ⇒ AccessToken.verifyAndDecode(at)
        case None     ⇒ Left(TokenMissing)
      }
    case None ⇒ AccessToken.verifyAndDecode(keycloakInstalled.getTokenString)
  }

  private def accessTokenStr() = authStore match {
    case Some(store) ⇒ store.getAccessTokenString
    case None        ⇒ Option(keycloakInstalled.getTokenString)
  }

  private def isExpired(accessToken: AccessToken, minValidity: FiniteDuration) =
    accessToken.exp match {
      case Some(exp) ⇒ (exp * 1000 - minValidity.toMillis) < System.currentTimeMillis
      case None      ⇒ false
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
