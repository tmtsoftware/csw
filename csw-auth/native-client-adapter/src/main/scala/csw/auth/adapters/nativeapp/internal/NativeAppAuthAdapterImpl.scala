package csw.auth.adapters.nativeapp.internal

import csw.auth.adapters.nativeapp.api.{AuthStore, NativeAppAuthAdapter}
import csw.auth.core.TokenVerificationFailure.TokenExpired
import csw.auth.core.TokenVerifier
import csw.auth.core.token.AccessToken
import org.keycloak.adapters.KeycloakDeployment
import org.keycloak.adapters.installed.KeycloakInstalled

import scala.concurrent.duration.{DurationLong, FiniteDuration}

private[auth] class NativeAppAuthAdapterImpl(
    val keycloakInstalled: KeycloakInstalled,
    maybeStore: Option[AuthStore] = None,
    tokenVerifier: TokenVerifier = TokenVerifier()
) extends NativeAppAuthAdapter {

  def this(keycloakDeployment: KeycloakDeployment) = this(new KeycloakInstalled(keycloakDeployment))

  def this(keycloakDeployment: KeycloakDeployment, authStore: AuthStore) =
    this(new KeycloakInstalled(keycloakDeployment), Some(authStore))

  override def login(): Unit = {
    keycloakInstalled.login()
    updateAuthStore()
  }

  override def logout(): Unit = {
    keycloakInstalled.logout()
    clearAuthStore()
  }

  override def loginDesktop(): Unit = {
    keycloakInstalled.loginDesktop()
    updateAuthStore()
  }

  override def loginManual(): Unit = {
    keycloakInstalled.loginManual()
    updateAuthStore()
  }

  override def loginCommandLine(): Boolean = {
    val bool = keycloakInstalled.loginCommandLine()
    if (bool) updateAuthStore()
    bool
  }

  override def loginCommandLine(redirectUri: String): Boolean = {
    val bool = keycloakInstalled.loginCommandLine(redirectUri)
    if (bool) updateAuthStore()
    bool
  }

  override def getAccessTokenString(minValidity: FiniteDuration = 0.seconds): Option[String] = {
    getAccessToken(minValidity)
    accessTokenStr
  }

  override def getAccessToken(minValidity: FiniteDuration = 0.seconds): Option[AccessToken] = {
    def getNewToken: Option[AccessToken] = {
      refreshAccessToken()
      accessTokenStr.flatMap(tokenVerifier.verifyAndDecode(_).toOption)
    }

    val mayBeAccessTokenVerification = maybeStore match {
      case Some(store) => store.getAccessTokenString.map(tokenVerifier.verifyAndDecode)
      case None        => Some(tokenVerifier.verifyAndDecode(keycloakInstalled.getTokenString))
    }

    mayBeAccessTokenVerification flatMap {
      case Right(at)          => if (isExpired(at, minValidity)) getNewToken else Some(at)
      case Left(TokenExpired) => getNewToken
      case _                  => None
    }
  }

  private def isExpired(accessToken: AccessToken, minValidity: FiniteDuration) =
    accessToken.exp.exists(x ⇒ (x * 1000 - minValidity.toMillis) < System.currentTimeMillis)

  private def refreshAccessToken(): Unit = {
    refreshTokenStr.foreach(keycloakInstalled.refreshToken)
    updateAuthStore()
  }

  private def accessTokenStr = maybeStore match {
    case Some(store) ⇒ store.getAccessTokenString
    case None        ⇒ Option(keycloakInstalled.getTokenString)
  }

  private def refreshTokenStr = maybeStore match {
    case Some(store) ⇒ store.getRefreshTokenString
    case None        ⇒ Option(keycloakInstalled.getRefreshToken)
  }

  private def updateAuthStore(): Unit = {
    val response = keycloakInstalled.getTokenResponse
    maybeStore.foreach(_.saveTokens(response.getIdToken, response.getToken, response.getRefreshToken))
  }

  private def clearAuthStore(): Unit = maybeStore.foreach(_.clearStorage())
}
