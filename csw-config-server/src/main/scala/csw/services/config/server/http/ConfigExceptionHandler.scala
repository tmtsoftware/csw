package csw.services.config.server.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, ExceptionHandler}
import csw.services.config.api.exceptions.{FileAlreadyExists, FileNotFound, InvalidInput}
import csw.services.config.server.commons.ConfigServerLogger

import scala.util.control.NonFatal

/**
 * Maps server side exceptions to Http Status codes
 */
class ConfigExceptionHandler extends Directives with ConfigServerLogger.Simple {

  val exceptionHandler: ExceptionHandler = ExceptionHandler {
    case ex: FileAlreadyExists ⇒
      log.error(ex.getMessage, ex = ex)
      complete(StatusCodes.Conflict → ex.getMessage)
    case ex: FileNotFound ⇒
      log.error(ex.getMessage, ex = ex)
      complete(StatusCodes.NotFound → ex.getMessage)
    case ex: InvalidInput ⇒
      log.error(ex.getMessage, ex = ex)
      complete(StatusCodes.BadRequest → ex.getMessage)
    case NonFatal(ex) ⇒
      log.error(ex.getMessage, ex = ex)
      complete(StatusCodes.InternalServerError → ex.getMessage)
  }
}
