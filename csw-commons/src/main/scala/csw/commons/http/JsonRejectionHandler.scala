package csw.commons.http

import akka.http.scaladsl.model.{HttpEntity, HttpResponse}
import akka.http.scaladsl.server.RejectionHandler
import csw.commons.http.JsonSupport.asJsonEntity

trait JsonRejectionHandler {

  implicit def jsonRejectionHandler: RejectionHandler =
    RejectionHandler.default
      .mapRejectionResponse {
        case response @ HttpResponse(status, _, entity: HttpEntity.Strict, _) =>
          // since all Akka default rejection responses are Strict this will handle all rejections
          val message = entity.data.utf8String.replaceAll("\"", """\"""")
          response.withEntity(asJsonEntity(status.intValue, message))
        case x => x
      }
}
