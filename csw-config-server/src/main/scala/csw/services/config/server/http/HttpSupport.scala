package csw.services.config.server.http

import java.nio.file.{Path, Paths}
import java.time.Instant

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.headers.HttpEncoding
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server._
import csw.services.config.api.models.{ConfigData, ConfigId, JsonSupport}

trait HttpSupport extends Directives with JsonSupport {
  val pathParam: Directive1[Path]              = parameter('path).map(filePath ⇒ Paths.get(filePath))
  val latestParam: Directive1[Boolean]         = parameter('latest.as[Boolean] ? false)
  val idParam: Directive1[Option[ConfigId]]    = parameter('id.?).map(_.map(new ConfigId(_)))
  val dateParam: Directive1[Option[Instant]]   = parameter('date.?).map(_.map(Instant.parse))
  val maxResultsParam: Directive1[Int]         = parameter('maxResults.as[Int] ? Int.MaxValue)
  val patternParam: Directive1[Option[String]] = parameter('pattern.?)
  val typeParam: Directive1[Option[String]]    = parameter('type.?)
  val commentParam: Directive1[String]         = parameter('comment ? "")
  val annexParam: Directive1[Boolean]          = parameter('annex.as[Boolean] ? false)
  val FilePath: PathMatcher1[Path]             = Remaining.map(path => Paths.get(path))

  val rejectMissingContentLength: Directive0 = extractRequestEntity.flatMap {
    case entity if entity.contentLengthOption.isDefined ⇒ pass
    case _ ⇒
      reject(UnsupportedRequestEncodingRejection(HttpEncoding("all encodings with contentLength are supported")))
  }

  //This marshaller is used to create a response chunked stream for get/getDefault requests
  implicit val configDataMarshaller: ToEntityMarshaller[ConfigData] = Marshaller.opaque { configData =>
    HttpEntity(ContentTypes.`application/octet-stream`, configData.length, configData.source)
  }

  val configDataEntity: Directive1[ConfigData] = rejectMissingContentLength & extractRequestEntity
    .map(entity => ConfigData.from(entity.dataBytes, entity.contentLengthOption.get))
}
