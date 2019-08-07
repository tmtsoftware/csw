package csw.commons.http

import akka.http.scaladsl.model._
import akka.util.ByteString
import io.bullet.borer.Json
import csw.commons.http.codecs.ErrorCodecs._
import io.bullet.borer.compat.akka._

/**
 * Internal API used by HTTP servers for exception handling.
 */
object JsonSupport {

  def asJsonEntity(statusCode: StatusCode, message: String): HttpEntity.Strict = {
    val errorResponse = ErrorResponse(ErrorMessage(statusCode.intValue, message))
    HttpEntity(ContentTypes.`application/json`, Json.encode(errorResponse).to[ByteString].result)
  }

  def asJsonResponse(statusCode: StatusCode, message: String): HttpResponse =
    HttpResponse(statusCode, entity = asJsonEntity(statusCode.intValue, message))
}
