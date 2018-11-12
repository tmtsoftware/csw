package csw.auth.api

import org.keycloak.representations.AccessToken

import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.util.Try

trait KeycloakInstalledApi {

  /**
   * Automatic login using the best approach
   */
  def login(): Unit

  /**
   * Logout and delete all tokens
   */
  def logout(): Unit

  /**
   * Login using a browser
   */
  def loginDesktop(): Unit

  /**
   * Login manually by using external browser and
   * copying the access code from there into this app
   */
  def loginManual(): Unit

  /**
   * User logs in by entering credentials on console. This however is not Resource Owner Credentials Password grant.
   * This method uses www-authenticate mechanism to post user credentials to auth server. It can also work with OTP.
   * @param redirectUri
   * @return
   */
  def loginCommandLine(redirectUri: String = "urn:ietf:wg:oauth:2.0:oob"): Boolean

  /**
   * Access token in string format
   * @param minValidity ensure that returned access token is valid at-least for given duration.
   *                    Will refresh the token if required.
   * @return access token in string format. This token could be signed or encrypted
   */
  def getAccessTokenString(minValidity: FiniteDuration = 0.seconds): Try[String]

  /**
   * Access token
   *
   * @param minValidity ensure that returned access token is valid at-least for given duration.
   *                    Will refresh the token if required.
   * @return
   */
  def getAccessToken(minValidity: FiniteDuration = 0.seconds): Try[AccessToken]
}
