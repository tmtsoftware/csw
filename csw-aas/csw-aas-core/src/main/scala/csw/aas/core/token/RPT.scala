package csw.aas.core.token
import cats.data.EitherT
import csw.aas.core.TokenVerificationFailure
import csw.aas.core.TokenVerificationFailure.InvalidToken
import csw.aas.core.commons.AuthLogger
import org.keycloak.authorization.client.AuthzClient
import pdi.jwt.{JwtJson, JwtOptions}

import scala.concurrent.{blocking, ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

private[aas] class RPT(authzClient: AuthzClient)(implicit ec: ExecutionContext) {
  private val logger = AuthLogger.getLogger
  import logger._

  def create(token: String): EitherT[Future, TokenVerificationFailure, AccessToken] = {
    debug("fetching RPT from keycloak")

    val rptStringF = getAuthorizationResponse(token)

    rptStringF.onComplete {
      case Success(_) => debug("fetched RPT string from keycloak")
      case Failure(e) => error("error while fetching RPT string from keycloak", ex = e)
    }

    EitherT(rptStringF.map(decodeRPT))
  }

  private def decodeRPT(rptString: String): Either[TokenVerificationFailure, AccessToken] =
    JwtJson
      .decodeJson(rptString, JwtOptions(signature = false, expiration = false, notBefore = false))
      .toLeftMappedEither("error while decoding RPT")
      .flatMap { js =>
        Try { js.as[AccessToken] }.toLeftMappedEither("error while mapping RPT json to access token")
      }
      .map { at =>
        debug(s"successfully fetched RPT from keycloak for ${at.userOrClientName}")
        at
      }

  implicit private class TryOps[T](tryValue: Try[T]) {
    def toLeftMappedEither(logMsg: String): Either[InvalidToken, T] = tryValue.toEither.left.map {
      case NonFatal(e) => logger.error(logMsg, ex = e); InvalidToken(e)
    }
  }

  private def getAuthorizationResponse(token: String): Future[String] = Future {
    blocking { authzClient.authorization(token).authorize().getToken }
  }
}

object RPT {
  def apply(authzClient: AuthzClient)(implicit ec: ExecutionContext): RPT = new RPT(authzClient)
}
