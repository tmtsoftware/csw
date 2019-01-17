package csw.config.server.http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import csw.config.server.commons.ConfigServerLogger
import csw.logging.api.scaladsl.Logger

trait TokenMaskSupport {
  private val log: Logger      = ConfigServerLogger.getLogger
  private val maskedToken      = "**********"
  private val maskedAuthHeader = Authorization(OAuth2BearerToken(maskedToken))

  val maskRequest: HttpRequest ⇒ HttpRequest = req ⇒
    req.header[Authorization] match {
      case Some(_) ⇒ req.removeHeader(Authorization.name).addHeader(maskedAuthHeader)
      case None    ⇒ req
  }

  val logRequest: HttpRequest ⇒ Unit = req ⇒
    log.info(
      "HTTP request received",
      Map(
        "url"     → req.uri.toString(),
        "method"  → req.method.value.toString,
        "headers" → req.headers.mkString(",")
      )
  )
}
