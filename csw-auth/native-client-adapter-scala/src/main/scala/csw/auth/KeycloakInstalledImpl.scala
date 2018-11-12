package csw.auth

import java.awt.Desktop
import java.io._

import csw.auth.api.KeycloakInstalledApi
import org.keycloak.adapters.KeycloakDeployment
import org.keycloak.adapters.installed.KeycloakInstalled
import org.keycloak.representations.AccessToken

import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.util.{Failure, Success, Try}

private[auth] class KeycloakInstalledImpl(val keycloakInstalled: KeycloakInstalled) extends KeycloakInstalledApi {

  object Constants {
    val `WWW-Authenticate`: String = "www-authenticate"
    val maxRedirectCount: Int      = 4
    val maxPasswordAttempts: Int   = 3
  }

  def this() {
    this(
      new KeycloakInstalled(
        Thread
          .currentThread()
          .getContextClassLoader
          .getResourceAsStream("META-INF/keycloak.json")
      )
    )
  }

  def this(config: InputStream) {
    this(new KeycloakInstalled(config))
  }

  def this(deployment: KeycloakDeployment) {
    this(new KeycloakInstalled(deployment))
  }

  def login(): Unit = {
    if (Desktop.isDesktopSupported) loginDesktop()
    else loginManual()
  }

  def logout(): Unit = keycloakInstalled.logout()

  def loginDesktop(): Unit =
    keycloakInstalled.loginDesktop()

  def loginManual(): Unit =
    loginManual(System.out, new InputStreamReader(System.in))

  private def loginManual(printer: PrintStream, reader: Reader): Unit = keycloakInstalled.loginManual(printer, reader)

  def loginCommandLine(redirectUri: String = "urn:ietf:wg:oauth:2.0:oob"): Boolean =
    keycloakInstalled.loginCommandLine(redirectUri)

  private implicit class OptionOps[A](opt: Option[A]) {

    def toTry(msg: String): Try[A] = {
      opt
        .map(Success(_))
        .getOrElse(Failure(new RuntimeException(msg)))
    }
  }

  private def hasExpired(seconds: Long) = { seconds < System.currentTimeMillis }

  def getAccessTokenString(minValidity: FiniteDuration = 0.seconds): Try[String] = {

    Option(keycloakInstalled.getToken)
      .map { at =>
        val exp = at.getExpiration
          .asInstanceOf[Long] * 1000 - minValidity.toMillis
        if (hasExpired(exp)) refreshAccessToken()
        keycloakInstalled.getTokenString
      }
      .toTry("Access token not found")
  }

  def getAccessToken(minValidity: FiniteDuration = 0.seconds): Try[AccessToken] = {

    Option(keycloakInstalled.getToken)
      .map(at => {
        val exp = at.getExpiration
        if (hasExpired(exp)) refreshAccessToken()
        keycloakInstalled.getToken
      })
      .toTry("Access token not found")
  }

  private def refreshAccessToken(): Unit = keycloakInstalled.refreshToken()

}
