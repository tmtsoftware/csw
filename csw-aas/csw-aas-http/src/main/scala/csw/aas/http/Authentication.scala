package csw.aas.http

import akka.http.scaladsl.server.Directives.AsyncAuthenticator
import akka.http.scaladsl.server.directives.Credentials.Provided
import csw.aas.core.commons.AuthLogger
import msocket.security.api.TokenValidator
import msocket.security.models.AccessToken

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

/**
 * Provides authentication to csw-aas-http security directives
 */
private[csw] class Authentication(tokenValidator: TokenValidator)(implicit ec: ExecutionContext) {

  private val logger = AuthLogger.getLogger

  /**
   * Returns an instance of AsyncAuthenticator
   * This is used to create AuthenticationDirective (akka-http)
   */
  def authenticator: AsyncAuthenticator[AccessToken] = {
    case Provided(token) =>
      tokenValidator
        .validate(token)
        .map { accessToken =>
          logger.debug(s"authentication successful for ${accessToken.userOrClientName}")
          Some(accessToken)
        }
        .recover {
          case NonFatal(ex) =>
            logger.debug(s"authentication failed due to error: ${ex.getMessage}")
            None
        }

    case _ =>
      logger.warn("authorization information is missing from request. authentication failed")
      Future.successful(None)
  }
}
