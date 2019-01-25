package csw.config.cli

import csw.aas.native.api.NativeAppAuthAdapter
import csw.config.api.TokenFactory

/**
 * Provides implementation for [[TokenFactory]] and uses [[NativeAppAuthAdapter]] underneath provided by `csw-aas-native`
 *
 * @param nativeAuthAdapter adapter which support login, storing and fetching tokens from [[csw.aas.native.api.AuthStore]]
 */
class CliTokenFactory(nativeAuthAdapter: NativeAppAuthAdapter) extends TokenFactory {

  override def getToken: String =
    nativeAuthAdapter
      .getAccessTokenString()
      .getOrElse(throw new RuntimeException("Missing access token, You must login before executing this command."))

}
