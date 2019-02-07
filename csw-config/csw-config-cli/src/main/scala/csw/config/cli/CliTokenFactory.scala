package csw.config.cli

import csw.aas.installed.api.InstalledAppAuthAdapter
import csw.config.api.TokenFactory

/**
 * Provides implementation for [[TokenFactory]] and uses [[InstalledAppAuthAdapter]] underneath provided by `csw-aas-installed`
 *
 * @param installedAuthAdapter adapter which support login, storing and fetching tokens from [[csw.aas.installed.api.AuthStore]]
 */
class CliTokenFactory(installedAuthAdapter: InstalledAppAuthAdapter) extends TokenFactory {

  override def getToken: String =
    installedAuthAdapter
      .getAccessTokenString()
      .getOrElse(throw new RuntimeException("Missing access token, You must login before executing this command."))

}
