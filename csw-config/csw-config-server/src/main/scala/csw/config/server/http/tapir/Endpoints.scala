package csw.config.server.http.tapir

import java.nio.file.{Path, Paths}
import java.time.Instant

import akka.stream.scaladsl.Source
import akka.util.ByteString
import csw.config.api.ConfigData
import csw.config.models.ConfigId
import csw.config.models.codecs.ConfigCodecs._
import csw.config.server.http.tapir.TapirJsonBorer._
import sttp.capabilities.akka.AkkaStreams
import sttp.model.StatusCode._
import sttp.tapir._

object QueryParams {
  val date: EndpointInput.Query[Option[Instant]] =
    query[Option[Instant]]("date")
      .description("Latest version of file on provided timestamp will be retrieved")

  val id: EndpointInput.Query[Option[ConfigId]] =
    query[Option[String]]("id")
      .description("Revision number of configuration file")
      .map(_.map(new ConfigId(_)))(id => id.map(_.id))

  val annex: EndpointInput.Query[Boolean] =
    query[Option[Boolean]]("annex")
      .map(_.getOrElse(false))(b => Some(b))
      .description("Flag to upload file in Annex store")

  val comment: EndpointInput.Query[String] =
    query[Option[String]]("comment").description("Commit message").map(_.getOrElse(""))(c => Some(c))

}

object Body {
  val configId: EndpointIO[ConfigId] = jsonBody[ConfigId]

  val fileStream: StreamBodyIO[Source[ByteString, Any], ConfigData, AkkaStreams] =
    streamBody(AkkaStreams, Schema.schemaForFile, CodecFormat.OctetStream())
      .description("Streaming file data")
      .map(bs => ConfigData.from(bs, 1))(c => c.source) // fixme: content-length = 1
}

object Endpoints {

  private val notFound = statusCode(NotFound).description("Not Found")

  private val pathsIn: EndpointInput.PathsCapture[Path] =
    paths.description("File path from repository").map(p => Paths.get(p.mkString("/")))(_.toString.split("/").toList)

  private val configEndpoint = endpoint.in("config").in(pathsIn)

  val getConfigEndpoint: Endpoint[(Path, Option[Instant], Option[ConfigId]), Unit, ConfigData, AkkaStreams] =
    configEndpoint.get
      .summary("Get Config")
      .description(
        "Fetches the latest version of requested configuration file from the repository either from normal/annex store"
      )
      .in(QueryParams.date.and(QueryParams.id))
      .out(Body.fileStream)
      .errorOut(notFound)

  val existEndpoint: Endpoint[(Path, Option[ConfigId]), Unit, Unit, Any] =
    configEndpoint.head
      .summary("Exist Config")
      .description("Checks if file exist in repository")
      .in(QueryParams.id)
      .out(emptyOutput)
      .errorOut(notFound)

  val createConfigEndpoint: Endpoint[(Path, String, Boolean, String, ConfigData), Unit, ConfigId, AkkaStreams] =
    configEndpoint.post
      .summary("Create Config")
      .description("""
          |Uploads configuration file in the repository.
          |Configuration file is stored in annex store if one of the below condition satisfies else stored in normal store:
          |1. Annex flag is true
          |2. File size exceeds the maximum size configured in Configuration service.
          |""".stripMargin)
      .in(auth.bearer[String])
      .in(QueryParams.annex.and(QueryParams.comment))
      .in(Body.fileStream)
      .out(statusCode(Created).and(Body.configId))

  val putConfigEndpoint: Endpoint[(Path, String, String, ConfigData), Unit, Unit, AkkaStreams] =
    configEndpoint.put
      .summary("Update Config")
      .description("configuration content to be updated in configuration service")
      .in(auth.bearer[String])
      .in(QueryParams.comment)
      .in(Body.fileStream)
      .out(emptyOutput)

  val deleteConfigEndpoint: Endpoint[(Path, String, String), Unit, Unit, Any] =
    configEndpoint.delete
      .summary("Delete Config")
      .description("Removes the configuration file from repository.")
      .in(auth.bearer[String])
      .in(QueryParams.comment)
      .out(emptyOutput)
}
