/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.config.api.commons
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import csw.logging.api.scaladsl.Logger

trait TokenMaskSupport {
  private[config] val logger: Logger
  private val maskedToken      = "**********"
  private val maskedAuthHeader = Authorization(OAuth2BearerToken(maskedToken))

  val maskRequest: HttpRequest => HttpRequest = req =>
    req.header[Authorization] match {
      case Some(_) => req.removeHeader(Authorization.name).addHeader(maskedAuthHeader)
      case None    => req
    }

  val logRequest: HttpRequest => Unit = req =>
    logger.info(
      "HTTP request received",
      Map(
        "url"     -> req.uri.toString(),
        "method"  -> req.method.value.toString,
        "headers" -> req.headers.mkString(",")
      )
    )
}
