package csw.commons.http

import akka.http.scaladsl.model._
import spray.json.pimpAny

object JsonSupport {

  def asJsonEntity(statusCode: StatusCode, message: String): HttpEntity.Strict = {
    val errorResponse = ErrorResponse(ErrorMessage(statusCode.intValue, message))
    HttpEntity(ContentTypes.`application/json`, errorResponse.toJson.prettyPrint)
  }

  def asJsonResponse(statusCode: StatusCode, message: String): HttpResponse =
    HttpResponse(statusCode, entity = asJsonEntity(statusCode.intValue, message))

}
