package csw.config.cli

import csw.aas.native.api.NativeAppAuthAdapter
import csw.config.api.TokenFactory

class CliTokenFactory(nativeAuthAdapter: NativeAppAuthAdapter) extends TokenFactory {

  override def getToken: String =
    nativeAuthAdapter
      .getAccessTokenString()
      .getOrElse(throw new RuntimeException("Missing access token, You must login before executing this command."))

}
