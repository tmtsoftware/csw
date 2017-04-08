package csw.services.config.server

import java.io.File
import java.nio.file.Paths

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.HttpEntity.Chunked
import akka.http.scaladsl.server.{Directive1, Directives}
import csw.services.config.models.{ConfigData, ConfigId, ConfigSource}

trait HttpSupport extends Directives with JsonSupport {
  val pathParam: Directive1[File] = parameter('path).map(filePath ⇒ Paths.get(filePath).toFile)
  val idParam: Directive1[Option[ConfigId]] = parameter('id.?).map(_.map(new ConfigId(_)))
  val maxResultsParam: Directive1[Int] = parameter('maxResults.as[Int] ? Int.MaxValue)
  val commentParam: Directive1[String] = parameter('comment ? "")
  val oversizeParam: Directive1[Boolean] = parameter('oversize.as[Boolean] ? false)
  val fileDataParam: Directive1[ConfigSource] = fileUpload("conf").map { case (_, source) ⇒ ConfigSource(source) }

  implicit val configDataMarshaller: ToEntityMarshaller[ConfigData] = Marshaller.opaque { configData =>
    Chunked.fromData(ContentTypes.`application/octet-stream`, configData.source)
  }
}
