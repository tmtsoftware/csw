package csw.apps.clusterseed.admin.http

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Directives, ExceptionHandler, RejectionHandler}
import csw.apps.clusterseed.admin.exceptions.UnresolvedAkkaOrHttpLocationException
import csw.apps.clusterseed.commons.ClusterSeedLogger
import spray.json.{pimpAny, RootJsonFormat}

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
class AdminHandlers extends Directives with ClusterSeedLogger.Simple {

  private def httpJsonEntity(statusCode: StatusCode, message: String): HttpEntity.Strict = {
    val errorResponse = ErrorResponse(ErrorMessage(statusCode.intValue, message))
    HttpEntity(ContentTypes.`application/json`, errorResponse.toJson.prettyPrint)
  }

  private def httpJsonResponse(statusCode: StatusCode, message: String): HttpResponse =
    HttpResponse(statusCode, entity = httpJsonEntity(statusCode.intValue, message))

  val jsonExceptionHandler: ExceptionHandler = ExceptionHandler {
    case ex: UnresolvedAkkaOrHttpLocationException ⇒
      log.error(ex.getMessage, ex = ex)
      complete(httpJsonResponse(StatusCodes.NotFound, ex.getMessage))
    case NonFatal(ex) ⇒
      log.error(ex.getMessage, ex = ex)
      complete(httpJsonResponse(StatusCodes.InternalServerError, ex.getMessage))
  }

  def jsonRejectionHandler: RejectionHandler =
    RejectionHandler.default
      .mapRejectionResponse {
        case response @ HttpResponse(status, _, entity: HttpEntity.Strict, _) =>
          // since all Akka default rejection responses are Strict this will handle all rejections
          val message = entity.data.utf8String.replaceAll("\"", """\"""")
          response.withEntity(httpJsonEntity(status.intValue, message))
        case x => x
      }
}
