package csw.apps.clusterseed.admin.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, ExceptionHandler}
import csw.apps.clusterseed.admin.exceptions.UnresolvedAkkaLocationException
import csw.apps.clusterseed.commons.ClusterSeedLogger

import scala.util.control.NonFatal

/**
 * Maps server side exceptions to Http Status codes
 */
class AdminExceptionHandler extends Directives with ClusterSeedLogger.Simple {

  val exceptionHandler: ExceptionHandler = ExceptionHandler {
    case ex: UnresolvedAkkaLocationException ⇒
      log.error(ex.getMessage, ex)
      complete(StatusCodes.NotFound → ex.getMessage)
    case NonFatal(ex) ⇒
      log.error(ex.getMessage, ex)
      complete(StatusCodes.InternalServerError → ex.getMessage)
  }
}
