package csw.config.server.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, ExceptionHandler}
import csw.commons.http.{JsonRejectionHandler, JsonSupport}
import csw.config.api.exceptions.{FileAlreadyExists, FileNotFound, InvalidInput}
import csw.config.server.commons.ConfigServerLogger
import csw.logging.api.scaladsl.Logger

import scala.util.control.NonFatal

/**
 * Maps server side exceptions to Http Status codes and sent to the client
 */
class ConfigHandlers extends Directives with JsonRejectionHandler {
  private val log: Logger = ConfigServerLogger.getLogger

  val jsonExceptionHandler: ExceptionHandler = ExceptionHandler {
    case ex: FileAlreadyExists ⇒
      log.error(ex.getMessage, ex = ex)
      complete(JsonSupport.asJsonResponse(StatusCodes.Conflict, ex.getMessage))
    case ex: FileNotFound ⇒
      log.error(ex.getMessage, ex = ex)
      complete(JsonSupport.asJsonResponse(StatusCodes.NotFound, ex.getMessage))
    case ex: InvalidInput ⇒
      log.error(ex.getMessage, ex = ex)
      complete(JsonSupport.asJsonResponse(StatusCodes.BadRequest, ex.getMessage))
    case NonFatal(ex) ⇒
      log.error(ex.getMessage, ex = ex)
      complete(JsonSupport.asJsonResponse(StatusCodes.InternalServerError, ex.getMessage))
  }
}
