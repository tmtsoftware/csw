package csw.services.config.server.http

import java.nio.file.{Path, Paths}
import java.time.Instant
import java.util.regex.{Pattern, PatternSyntaxException}

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.headers.HttpEncoding
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest}
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.{DebuggingDirectives, LoggingMagnet}
import csw.services.config.api.internal.JsonSupport
import csw.services.config.api.models.{ConfigData, ConfigId, FileType}
import csw.services.config.server.commons.{ConfigServerLogger, PathValidator}

/**
 * Helper class for ConfigServiceRoute
 */
trait HttpSupport extends Directives with JsonSupport with ConfigServerLogger.Simple {

  def prefix(prefix: String): Directive1[Path] = path(prefix / Remaining).flatMap { path =>
    validate(PathValidator.isValid(path), PathValidator.message(path)).tmap[Path] { _ =>
      Paths.get(path)
    }
  }

  private def logRequest(req: HttpRequest): Unit =
    log.info("HTTP request received",
      Map("url" → req.uri.toString(), "method" → req.method.value.toString, "headers" → req.headers.mkString(",")))
  val routeLogger: Directive0 = DebuggingDirectives.logRequest(LoggingMagnet(_ => logRequest))

  val pathParam: Directive1[Path]            = parameter('path).map(filePath ⇒ Paths.get(filePath))
  val latestParam: Directive1[Boolean]       = parameter('latest.as[Boolean] ? false)
  val idParam: Directive1[Option[ConfigId]]  = parameter('id.?).map(_.map(new ConfigId(_)))
  val dateParam: Directive1[Option[Instant]] = parameter('date.?).map(_.map(Instant.parse))
  val fromParam: Directive1[Instant]         = parameter('from.?).map(_.map(Instant.parse).getOrElse(Instant.MIN))
  val toParam: Directive1[Instant]           = parameter('to.?).map(_.map(Instant.parse).getOrElse(Instant.now()))
  val maxResultsParam: Directive1[Int]       = parameter('maxResults.as[Int] ? Int.MaxValue)
  val commentParam: Directive1[String]       = parameter('comment ? "")
  val annexParam: Directive1[Boolean]        = parameter('annex.as[Boolean] ? false)

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

  //This marshaller is used to create a response stream for get/getActive requests
  implicit val configDataMarshaller: ToEntityMarshaller[ConfigData] = Marshaller.opaque { configData =>
    HttpEntity(ContentTypes.`application/octet-stream`, configData.length, configData.source)
  }
}
