package csw.commons.http

import akka.http.scaladsl.model._
import play.api.libs.json.Json

//TODO: add more explanation
object JsonSupport {

  def asJsonEntity(statusCode: StatusCode, message: String): HttpEntity.Strict = {
    val errorResponse = ErrorResponse(ErrorMessage(statusCode.intValue, message))
    HttpEntity(ContentTypes.`application/json`, Json.prettyPrint(Json.toJson(errorResponse)))
  }

  def asJsonResponse(statusCode: StatusCode, message: String): HttpResponse =
    HttpResponse(statusCode, entity = asJsonEntity(statusCode.intValue, message))

}
