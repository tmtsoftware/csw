package csw.aas.http

import akka.http.scaladsl.server.Directives.Authenticator
import akka.http.scaladsl.server.directives.Credentials.Provided
import csw.aas.core.token.{AccessToken, TokenFactory}

class Authentication(tokenFactory: TokenFactory) {
  def authenticator: Authenticator[AccessToken] = {
    case Provided(token) ⇒ tokenFactory.makeToken(token).toOption
    case _               ⇒ None
  }
}
