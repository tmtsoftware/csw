package csw.auth

import java.io._

import csw.auth.api.KeycloakInstalledApi
import org.keycloak.adapters.KeycloakDeployment
import org.keycloak.adapters.installed.KeycloakInstalled
import org.keycloak.representations.AccessToken

import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.util.{Failure, Success, Try}

private[auth] class KeycloakInstalledImpl(val keycloakInstalled: KeycloakInstalled) extends KeycloakInstalledApi {

  def this() = this(new KeycloakInstalled())

  def this(config: InputStream) = this(new KeycloakInstalled(config))

  def this(deployment: KeycloakDeployment) = this(new KeycloakInstalled(deployment))

  def login(): Unit = keycloakInstalled.login()

  def logout(): Unit = keycloakInstalled.logout()

  def loginDesktop(): Unit = keycloakInstalled.loginDesktop()

  def loginManual(): Unit = keycloakInstalled.loginManual()

  def loginCommandLine(): Boolean = keycloakInstalled.loginCommandLine()

  def loginCommandLine(redirectUri: String): Boolean = keycloakInstalled.loginCommandLine(redirectUri)

  private implicit class OptionOps[A](opt: Option[A]) {

    def toTry(msg: String): Try[A] = {
      opt
        .map(Success(_))
        .getOrElse(Failure(new RuntimeException(msg)))
    }
  }

  def getAccessTokenString(minValidity: FiniteDuration = 0.seconds): Try[String] =
    Try(keycloakInstalled.getTokenString(minValidity.length, minValidity.unit))

  // todo : minValidity is not considered here.
  def getAccessToken(minValidity: FiniteDuration = 0.seconds): Try[AccessToken] =
    Option(keycloakInstalled.getToken)
      .map { at =>
        if (at.isExpired) refreshAccessToken()
        keycloakInstalled.getToken
      }
      .toTry("Access token not found")

  private def refreshAccessToken(): Unit = keycloakInstalled.refreshToken()

}
