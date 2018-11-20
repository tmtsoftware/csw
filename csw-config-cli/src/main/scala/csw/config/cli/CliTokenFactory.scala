package csw.config.cli

import csw.auth.adapters.nativeapp.api.NativeAppAuthAdapter
import csw.config.api.TokenFactory

class CliTokenFactory(nativeAuthAdapter: NativeAppAuthAdapter) extends TokenFactory {

  override def getToken: String =
    nativeAuthAdapter
      .getAccessTokenString()
      .getOrElse(throw new RuntimeException("Missing access token, login before using this API"))

}
