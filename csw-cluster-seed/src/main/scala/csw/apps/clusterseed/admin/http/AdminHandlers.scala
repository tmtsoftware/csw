package csw.apps.clusterseed.admin.http

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Directives, ExceptionHandler}
import csw.apps.clusterseed.admin.exceptions.UnresolvedAkkaOrHttpLocationException
import csw.apps.clusterseed.commons.ClusterSeedLogger
import csw.commons.http.{JsonRejectionHandler, JsonSupport}
import spray.json.RootJsonFormat

import scala.util.control.NonFatal

// Two classes are used just to wrap status code and error message inside "error" key in json representation
case class ErrorResponse(error: ErrorMessage)
case object ErrorResponse {
  import spray.json.DefaultJsonProtocol._
  implicit val errorResponseFormat: RootJsonFormat[ErrorResponse] = jsonFormat1(ErrorResponse.apply)
}

case class ErrorMessage(code: Int, message: String)
case object ErrorMessage {
  import spray.json.DefaultJsonProtocol._
  implicit val errorMessageFormat: RootJsonFormat[ErrorMessage] = jsonFormat2(ErrorMessage.apply)
}

/**
 * Maps server side exceptions to Http Status codes
 */
class AdminHandlers extends Directives with JsonRejectionHandler with ClusterSeedLogger.Simple {

  val jsonExceptionHandler: ExceptionHandler = ExceptionHandler {
    case ex: UnresolvedAkkaOrHttpLocationException ⇒
      log.error(ex.getMessage, ex = ex)
      complete(JsonSupport.asJsonResponse(StatusCodes.NotFound, ex.getMessage))
    case NonFatal(ex) ⇒
      log.error(ex.getMessage, ex = ex)
      complete(JsonSupport.asJsonResponse(StatusCodes.InternalServerError, ex.getMessage))
  }

}
