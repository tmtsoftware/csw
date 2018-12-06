package csw.aas.http

import akka.http.scaladsl.server.Directives.AsyncAuthenticator
import akka.http.scaladsl.server.directives.Credentials.Provided
import csw.aas.core.token.{AccessToken, TokenFactory}

import scala.concurrent.{ExecutionContext, Future}

private[csw] class Authentication(tokenFactory: TokenFactory)(implicit ec: ExecutionContext) {
  def authenticator: AsyncAuthenticator[AccessToken] = {
    case Provided(token) ⇒ tokenFactory.makeToken(token).map(Some(_))
    case _               ⇒ Future.successful(None)
  }
}
