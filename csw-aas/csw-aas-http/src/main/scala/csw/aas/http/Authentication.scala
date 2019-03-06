package csw.aas.http

import akka.http.scaladsl.server.Directives.AsyncAuthenticator
import akka.http.scaladsl.server.directives.Credentials.Provided
import csw.aas.core.commons.AuthLogger
import csw.aas.core.token.{AccessToken, TokenFactory}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Provides authentication to csw-aas-http security directives
 */
private[csw] class Authentication(tokenFactory: TokenFactory)(implicit ec: ExecutionContext) {

  private val logger = AuthLogger.getLogger
  import logger._

  /**
   * Returns an instance of AsyncAuthenticator
   * This is used to create AuthenticationDirective (akka-http)
   */
  def authenticator: AsyncAuthenticator[AccessToken] = {
    case Provided(token) ⇒
      tokenFactory.makeToken(token).map { eitherAT =>
        eitherAT.foreach(at => debug(s"authentication successful for ${at.userOrClientName}"))
        eitherAT.toOption
      }

    case _ ⇒
      warn("authorization information is missing from request. authentication failed")
      Future.successful(None)
  }
}
