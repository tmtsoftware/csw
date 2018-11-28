package csw.auth.adapters.akka.http

import akka.http.scaladsl.server.Directives.Authenticator
import akka.http.scaladsl.server.directives.Credentials.Provided
import csw.auth.core.token.{AccessToken, TokenFactory}

//todo: these checks need to be more strong. we need to check for exp, aud, etc. consider taking help from keycloak to other jwt-scala
class Authentication(tokenFactory: TokenFactory) {
  def authenticator: Authenticator[AccessToken] = {
    case Provided(token) ⇒ tokenFactory.makeToken(token).toOption
    case _               ⇒ None
  }
}
