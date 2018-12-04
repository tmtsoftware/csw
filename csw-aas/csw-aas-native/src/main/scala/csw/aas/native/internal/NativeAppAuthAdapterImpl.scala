package csw.aas.native.internal

import csw.aas.core.TokenVerificationFailure.TokenExpired
import csw.aas.core.TokenVerifier
import csw.aas.core.deployment.AuthConfig
import csw.aas.core.token.AccessToken
import csw.aas.native.api.{AuthStore, NativeAppAuthAdapter}
import org.keycloak.adapters.installed.KeycloakInstalled

import scala.concurrent.duration.{DurationLong, FiniteDuration}

private[aas] class NativeAppAuthAdapterImpl(
    val keycloakInstalled: KeycloakInstalled,
    tokenVerifier: TokenVerifier,
    maybeStore: Option[AuthStore] = None
) extends NativeAppAuthAdapter {

  def this(authConfig: AuthConfig) =
    this(new KeycloakInstalled(authConfig.getDeployment), TokenVerifier(authConfig))

  def this(authConfig: AuthConfig, authStore: AuthStore) =
    this(new KeycloakInstalled(authConfig.getDeployment), TokenVerifier(authConfig), Some(authStore))

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
      try refreshAccessToken()
      catch {
        case e: Exception ⇒
          throw new RuntimeException(s"Error in refreshing token: try login before executing this command ${e.getMessage}")
      }
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
