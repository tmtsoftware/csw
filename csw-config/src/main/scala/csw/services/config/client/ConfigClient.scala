package csw.services.config.client

import java.io.File
import java.nio.file.Paths
import java.util.Date

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.Multipart.FormData
import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import csw.services.config.ActorRuntime
import csw.services.config.models._
import csw.services.config.scaladsl.ConfigManager
import csw.services.config.server.JsonSupport
import csw.services.location.models.Location

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationDouble

class ConfigClient(location: Location, actorRuntime: ActorRuntime) extends ConfigManager with JsonSupport {
  import actorRuntime._

  override def name: String = "http-based-config-client"

  override def create(path: File, configData: ConfigData, oversize: Boolean, comment: String): Future[ConfigId] = {
    val entity = HttpEntity.IndefiniteLength(ContentTypes.`application/octet-stream`, configData.source)
    val formData: Multipart.FormData = FormData(FormData.BodyPart("conf", entity, Map("filename" → "")))
    val uri = Uri(location.uri.toString)
      .withPath(Path / "create")
      .withQuery(Query("path" → path.getPath, "oversize" → oversize.toString, "comment" → comment))

    Marshal(formData).to[RequestEntity].flatMap { entity =>
      val request = HttpRequest(HttpMethods.POST, uri = uri, entity = entity)
      Http().singleRequest(request).flatMap { response ⇒
        if(response.status == StatusCodes.OK)
          Unmarshal(response).to[ConfigId]
        else response.entity.toStrict(5.seconds).map(s ⇒ throw new RuntimeException(s.data.utf8String))
      }
    }
  }

  override def update(path: File, configData: ConfigData, comment: String): Future[ConfigId] = ???

  override def get(path: File, id: Option[ConfigId]): Future[Option[ConfigData]] = {
    val uri = Uri(location.uri.toString)
      .withPath(Path / "get")
      .withQuery(Query(Map("path" → path.getPath) ++ id.map(d ⇒ "id" → d.toString)))
    val request = HttpRequest(uri = uri)
    Http().singleRequest(request).map { response =>
      if(response.status == StatusCodes.OK)
        Some(ConfigSource(response.entity.dataBytes))
      else
        None
    }
  }

  override def get(path: File, date: Date): Future[Option[ConfigData]] = ???

  override def exists(path: File): Future[Boolean] = ???

  override def delete(path: File, comment: String): Future[Unit] = ???

  override def list(): Future[List[ConfigFileInfo]] = ???

  override def history(path: File, maxResults: Int): Future[List[ConfigFileHistory]] = ???

  override def setDefault(path: File, id: Option[ConfigId]): Future[Unit] = ???

  override def resetDefault(path: File): Future[Unit] = ???

  override def getDefault(path: File): Future[Option[ConfigData]] = ???
}
