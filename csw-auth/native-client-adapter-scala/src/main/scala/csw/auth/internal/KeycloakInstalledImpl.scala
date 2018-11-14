package csw.auth.internal

import java.io._

import csw.auth.AccessToken
import csw.auth.api.{AuthStore, KeycloakInstalledApi}
import org.keycloak.adapters.KeycloakDeployment
import org.keycloak.adapters.installed.KeycloakInstalled

import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.util.Try

private[auth] class KeycloakInstalledImpl(keycloakInstalled: KeycloakInstalled, authStore: Option[AuthStore] = None)
    extends KeycloakInstalledApi {

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
    Try(keycloakInstalled.getTokenString(minValidity.length, minValidity.unit))

  // todo : minValidity is not considered here.
  def getAccessToken(minValidity: FiniteDuration = 0.seconds): Try[AccessToken] =
    Try {
      authStore
        .map(_.getAccessToken(keycloakInstalled.getDeployment))
        .getOrElse(
          Option(keycloakInstalled.getToken)
            .map { at =>
              if (at.isExpired) refreshAccessToken()
              keycloakInstalled.getToken
            }
        )
        .getOrElse(throw new RuntimeException("Access token not found"))
    }

//    Try {
//    Option(keycloakInstalled.getToken)
//      .map { at =>
//        if (at.isExpired) refreshAccessToken()
//        keycloakInstalled.getToken
//      }
//      .getOrElse(throw new RuntimeException("Access token not found"))
//  }

  private def refreshAccessToken(): Unit = {
    keycloakInstalled.refreshToken()
    updateAuthStore()
  }

  private def updateAuthStore(): Unit =
    authStore.foreach(_.saveAccessTokenResponse(keycloakInstalled.getTokenResponse, keycloakInstalled.getDeployment))

  private def clearAuthStore(): Unit = authStore.foreach(_.clearStorage())
}
