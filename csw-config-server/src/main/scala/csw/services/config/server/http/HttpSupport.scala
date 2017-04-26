package csw.services.config.server.http

import java.nio.file.{Path, Paths}
import java.time.Instant

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.HttpEntity.Chunked
import akka.http.scaladsl.server.{Directive, Directive1, Directives, PathMatcher1}
import csw.services.config.api.models.{ConfigData, ConfigId, JsonSupport}

trait HttpSupport extends Directives with JsonSupport {
  val pathParam: Directive1[Path]              = parameter('path).map(filePath ⇒ Paths.get(filePath))
  val latestParam: Directive1[Boolean]         = parameter('latest.as[Boolean] ? false)
  val idParam: Directive1[Option[ConfigId]]    = parameter('id.?).map(_.map(new ConfigId(_)))
  val dateParam: Directive1[Option[Instant]]   = parameter('date.?).map(_.map(Instant.parse))
  val maxResultsParam: Directive1[Int]         = parameter('maxResults.as[Int] ? Int.MaxValue)
  val commentParam: Directive1[String]         = parameter('comment ? "")
  val oversizeParam: Directive1[Boolean]       = parameter('oversize.as[Boolean] ? false)
  val configDataEntity: Directive1[ConfigData] = extractDataBytes.map(ConfigData.fromSource)
  val FilePath: PathMatcher1[Path]             = Remaining.map(path => Paths.get(path))

  //This marshaller is used to create a response chunked stream for get/getDefault requests
  implicit val configDataMarshaller: ToEntityMarshaller[ConfigData] = Marshaller.opaque { configData =>
    Chunked.fromData(ContentTypes.`application/octet-stream`, configData.source)
  }
}
