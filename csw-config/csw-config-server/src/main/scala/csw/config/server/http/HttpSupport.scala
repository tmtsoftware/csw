package csw.config.server.http

import java.nio.file.{Path, Paths}
import java.time.Instant
import java.util.regex.{Pattern, PatternSyntaxException}

import akka.http.javadsl.model.headers.Authorization
import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.headers.HttpEncoding
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpHeader, HttpRequest}
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.{DebuggingDirectives, LoggingMagnet}
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.util.ByteString
import csw.config.api.internal.JsonSupport
import csw.config.api.models.{ConfigData, ConfigId, FileType}
import csw.config.server.commons.{ConfigServerLogger, PathValidator}
import csw.logging.scaladsl.Logger
import csw.params.extensions.OptionConverters.RichOptional

/**
 * Helper class for ConfigServiceRoute
 */
trait HttpSupport extends Directives with JsonSupport {
  private val log: Logger         = ConfigServerLogger.getLogger
  private val authorizationHeader = "authorization"
  private val maskedToken         = "***********"

  def prefix(prefix: String): Directive1[Path] = path(prefix / Remaining).flatMap { path =>
    validate(PathValidator.isValid(path), PathValidator.message(path)).tmap[Path] { _ =>
      Paths.get(path)
    }
  }

  // log every request when received at HttpServer
  private def logRequest(req: HttpRequest): Unit = {
    val maskedHeader: Option[HttpHeader] = req
      .getHeader(authorizationHeader)
      .asScala
      .map(_ ⇒ Authorization.oauth2(maskedToken))

    val maskedReq = maskedHeader match {
      case Some(header) ⇒ req.removeHeader(authorizationHeader).addHeader(header)
      case None         ⇒ req
    }

    log.info(
      "HTTP request received",
      Map("url"     → maskedReq.uri.toString(),
          "method"  → maskedReq.method.value.toString,
          "headers" → maskedReq.headers.mkString(","))
    )
  }
  val routeLogger: Directive0 = DebuggingDirectives.logRequest(LoggingMagnet(_ => logRequest))

  val idParam: Directive1[Option[ConfigId]]  = parameter('id.?).map(_.map(new ConfigId(_)))
  val dateParam: Directive1[Option[Instant]] = parameter('date.?).map(_.map(Instant.parse))
  val fromParam: Directive1[Instant]         = parameter('from.?).map(_.map(Instant.parse).getOrElse(Instant.MIN))
  val toParam: Directive1[Instant]           = parameter('to.?).map(_.map(Instant.parse).getOrElse(Instant.now()))
  val maxResultsParam: Directive1[Int]       = parameter('maxResults.as[Int] ? Int.MaxValue)
  val commentParam: Directive1[String]       = parameter('comment ? "")
  val annexParam: Directive1[Boolean]        = parameter('annex.as[Boolean] ? false)

  // pattern is an optional parameter coming with list request
  // for list request if the pattern is provided, then it is first compiled and then forwarded to business code to process the request
  // if the pattern provided throws `PatternSyntaxException` then immediate response of `BadRequest` is sent back to client
  val patternParam: Directive1[Option[String]] = parameter('pattern.?).flatMap {
    case p @ Some(pattern) ⇒
      try {
        Pattern.compile(pattern)
        provide(p)
      } catch {
        case ex: PatternSyntaxException ⇒ reject(MalformedQueryParamRejection("pattern", ex.getMessage))
      }
    case None ⇒ provide(None)
  }

  // type is an optional parameter coming with list request
  // for list request if the type is provided, then it is first casted to one of the available types ('Annex' and 'Normal')
  // and then forwarded to business code to process the request
  // if the type provided throws `PatternSyntaxException` then immediate response of `BadRequest` is sent back to client
  val typeParam: Directive1[Option[FileType]] = parameter('type.?).flatMap {
    case Some(fileType) ⇒
      FileType.withNameInsensitiveOption(fileType) match {
        case ft @ Some(_) ⇒ provide(ft)
        case None         ⇒ reject(MalformedQueryParamRejection("type", s"Supported types: ${FileType.stringify}"))
      }
    case None ⇒ provide(None)
  }

  val configDataEntity: Directive1[ConfigData] = extractRequestEntity.flatMap {
    case entity if entity.contentLengthOption.isDefined ⇒
      provide(ConfigData.from(entity.dataBytes, entity.contentLengthOption.get))
    case _ ⇒
      reject(UnsupportedRequestEncodingRejection(HttpEncoding("All encodings with contentLength value")))
  }

  // This marshaller is used to create a response stream for get/getActive requests
  implicit val configDataMarshaller: ToEntityMarshaller[ConfigData] = Marshaller.opaque { configData =>
    HttpEntity(ContentTypes.`application/octet-stream`, configData.length, configData.source)
  }

  implicit val configDataUnmarshaller: FromEntityUnmarshaller[String] =
    Unmarshaller.byteStringUnmarshaller
      .forContentTypes(ContentTypes.`application/octet-stream`)
      .mapWithCharset {
        case (ByteString.empty, _) => throw Unmarshaller.NoContentException
        case (data, charset)       => data.decodeString(charset.nioCharset.name)
      }
}
