package csw.services.config.server.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, ExceptionHandler}
import csw.services.config.api.exceptions.{FileAlreadyExists, FileNotFound, InvalidInput}

import scala.util.control.NonFatal

class ConfigExceptionHandler extends Directives {

  val exceptionHandler: ExceptionHandler = ExceptionHandler {
    case ex: FileAlreadyExists ⇒
      ex.printStackTrace()
      complete(StatusCodes.Conflict → ex.getMessage)
    case ex: FileNotFound ⇒
      ex.printStackTrace()
      complete(StatusCodes.NotFound → ex.getMessage)
    case ex: InvalidInput ⇒
      ex.printStackTrace()
      complete(StatusCodes.BadRequest → ex.getMessage)
    case NonFatal(ex) ⇒
      ex.printStackTrace()
      complete(StatusCodes.InternalServerError → ex.getMessage)
  }
}
