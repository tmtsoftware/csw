/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.commons.http

import org.apache.pekko.http.scaladsl.model.{HttpEntity, HttpResponse}
import org.apache.pekko.http.scaladsl.server.RejectionHandler
import csw.commons.http.JsonSupport.asJsonEntity

/**
 * Internal API used by http servers to handle exceptions.
 */
trait JsonRejectionHandler {

  implicit def jsonRejectionHandler: RejectionHandler =
    RejectionHandler.default
      .mapRejectionResponse {
        case response @ HttpResponse(status, _, entity: HttpEntity.Strict, _) =>
          // since all Pekko default rejection responses are Strict this will handle all rejections
          val message = entity.data.utf8String.replaceAll("\"", """\"""")
          response.withEntity(asJsonEntity(message))
        case x => x
      }
}
