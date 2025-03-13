/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.commons.http

import org.apache.pekko.http.scaladsl.model.*
import org.apache.pekko.util.ByteString
import io.bullet.borer.Json
import csw.commons.http.codecs.ErrorCodecs.*
import io.bullet.borer.compat.pekko.*

/**
 * Internal API used by HTTP servers for exception handling.
 */
object JsonSupport {

  def asJsonEntity(message: String, errorName: Option[String] = None): HttpEntity.Strict = {
    val errorResponse = ErrorResponse(ErrorMessage(message, errorName))
    HttpEntity(ContentTypes.`application/json`, Json.encode(errorResponse).to[ByteString].result)
  }

  def asJsonResponse(statusCode: StatusCode, errorName: String, message: String): HttpResponse =
    HttpResponse(statusCode, entity = asJsonEntity(message, Some(errorName)))
}
