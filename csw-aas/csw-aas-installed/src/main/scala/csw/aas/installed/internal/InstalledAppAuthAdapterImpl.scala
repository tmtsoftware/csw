package csw.aas.installed.internal

import csw.aas.core.TokenVerificationFailure.TokenExpired
import csw.aas.core.TokenVerifier
import csw.aas.core.token.AccessToken
import csw.aas.core.utils.Conversions.RichEitherTFuture
import csw.aas.installed.api.{AuthStore, InstalledAppAuthAdapter}
import org.keycloak.adapters.installed.KeycloakInstalled

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.util.Try

private[aas] class InstalledAppAuthAdapterImpl(
    val keycloakInstalled: KeycloakInstalled,
    tokenVerifier: TokenVerifier,
    maybeStore: Option[AuthStore] = None
)(implicit executionContext: ExecutionContext)
    extends InstalledAppAuthAdapter {

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
      Try(refreshAccessToken()).recover {
        case e: Exception ⇒
          throw new RuntimeException(s"Error in refreshing token: try login before executing this command ${e.getMessage}")
      }

      accessTokenStr.flatMap(atr => tokenVerifier.verifyAndDecode(atr).block().toOption)
    }

    val mayBeAccessTokenVerification = maybeStore match {
      case Some(store) => store.getAccessTokenString.map(tokenVerifier.verifyAndDecode(_).block())
      case None        => Some(tokenVerifier.verifyAndDecode(keycloakInstalled.getTokenString).block())
    }

    mayBeAccessTokenVerification.flatMap {
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

  private def accessTokenStr  = queryToken(_.getAccessTokenString, keycloakInstalled.getTokenString)
  private def refreshTokenStr = queryToken(_.getRefreshTokenString, keycloakInstalled.getRefreshToken)

  private def queryToken(withStore: AuthStore ⇒ Option[String], withoutStore: ⇒ String) = maybeStore match {
    case Some(store) ⇒ withStore(store)
    case None        ⇒ Option(withoutStore)
  }

  private def updateAuthStore(): Unit = {
    val response = keycloakInstalled.getTokenResponse
    maybeStore.foreach(_.saveTokens(response.getIdToken, response.getToken, response.getRefreshToken))
  }

  private def clearAuthStore(): Unit = maybeStore.foreach(_.clearStorage())
}
