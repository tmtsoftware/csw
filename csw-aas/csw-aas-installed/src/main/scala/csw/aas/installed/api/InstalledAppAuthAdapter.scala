/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.aas.installed.api

import msocket.security.models.AccessToken

import scala.concurrent.duration.{DurationLong, FiniteDuration}

trait InstalledAppAuthAdapter {

  /**
   * Automatic login using the best approach (either via browser or manual login)
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
   *
   * @return login successful or not
   */
  def loginCommandLine(): Boolean

  /**
   * User logs in by entering credentials on console. This however is not Resource Owner Credentials Password grant.
   * This method uses www-authenticate mechanism to post user credentials to auth server. It can also work with OTP.
   *
   * @param redirectUri callback uri where you want to receive token response from auth server
   * @return login successful or not
   */
  def loginCommandLine(redirectUri: String): Boolean

  /**
   * Get [[msocket.security.models.AccessToken]] converted from raw access token string fetched from [[AuthStore]]
   *
   * @param minValidity ensure that returned access token is valid at-least for given duration.
   *                    Will refresh the token if required.
   * @return access token converted into [[msocket.security.models.AccessToken]] model if raw access token string is present in [[AuthStore]]
   */
  def getAccessToken(minValidity: FiniteDuration = 0.seconds): Option[AccessToken]

}
