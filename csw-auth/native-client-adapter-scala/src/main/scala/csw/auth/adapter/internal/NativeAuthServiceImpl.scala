package csw.auth.adapter.internal

import java.io._

import csw.auth.adapter.api.{AuthStore, NativeAuthService}
import org.keycloak.adapters.KeycloakDeployment
import org.keycloak.adapters.installed.KeycloakInstalled
import org.keycloak.representations.AccessToken

import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.util.Try

private[auth] class NativeAuthServiceImpl(keycloakInstalled: KeycloakInstalled, authStore: Option[AuthStore] = None)
    extends NativeAuthService {

  def this() = this(new KeycloakInstalled())

  def this(authStore: AuthStore) = this(new KeycloakInstalled(), Some(authStore))

  def this(config: InputStream) = this(new KeycloakInstalled(config))

  def this(deployment: KeycloakDeployment) = this(new KeycloakInstalled(deployment))

  def this(deployment: KeycloakDeployment, authStore: AuthStore) = this(new KeycloakInstalled(deployment), Some(authStore))

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
    Try {
      accessToken()
        .flatMap { token ⇒
          if (isExpired(token, minValidity)) { refreshAccessToken(); accessToken() } else Some(token)
        }
        .getOrElse(throw new RuntimeException("Access token not found"))
    }

  private def accessToken() = authStore match {
    case Some(store) ⇒ store.getAccessToken(keycloakInstalled.getDeployment)
    case None        ⇒ Option(keycloakInstalled.getToken)
  }

  private def accessTokenStr() = authStore match {
    case Some(store) ⇒ store.getAccessTokenString
    case None        ⇒ Option(keycloakInstalled.getTokenString)
  }

  private def isExpired(accessToken: AccessToken, minValidity: FiniteDuration) = {
    val expires: Long = accessToken.getExpiration.toLong * 1000 - minValidity.toMillis
    expires < System.currentTimeMillis
  }

  private def refreshAccessToken(): Unit = {
    keycloakInstalled.refreshToken()
    updateAuthStore()
  }

  private def updateAuthStore(): Unit =
    authStore.foreach(_.saveAccessTokenResponse(keycloakInstalled.getTokenResponse, keycloakInstalled.getDeployment))

  private def clearAuthStore(): Unit = authStore.foreach(_.clearStorage())
}
