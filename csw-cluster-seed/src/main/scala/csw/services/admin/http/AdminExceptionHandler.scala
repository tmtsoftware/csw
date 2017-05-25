package csw.services.admin.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, ExceptionHandler}
import csw.services.admin.exceptions.InvalidComponentNameException

import scala.util.control.NonFatal

/**
 * Maps server side exceptions to Http Status codes
 */
class AdminExceptionHandler extends Directives {

  val exceptionHandler: ExceptionHandler = ExceptionHandler {
    case ex: InvalidComponentNameException ⇒
      ex.printStackTrace()
      complete(StatusCodes.BadRequest → ex.getMessage)
    case NonFatal(ex) ⇒
      ex.printStackTrace()
      complete(StatusCodes.InternalServerError → ex.getMessage)
  }
}
