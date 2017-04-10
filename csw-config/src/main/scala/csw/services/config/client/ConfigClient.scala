package csw.services.config.client

import java.io.{File, FileNotFoundException, IOException}
import java.nio.file.Paths
import java.util.Date

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.Multipart.FormData
import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import csw.services.config.api.commons.ActorRuntime
import csw.services.config.api.models._
import csw.services.config.api.scaladsl.ConfigManager
import csw.services.config.server.http.JsonSupport
import csw.services.location.models.Location

import scala.concurrent.{Await, Future}

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
      println(request)
      Http().singleRequest(request).flatMap { response ⇒
        response.status match {
          case StatusCodes.OK         ⇒ Unmarshal(response).to[ConfigId]
          case StatusCodes.BadRequest ⇒ throw new IOException(response.status.reason())
          case _                      ⇒ throw new RuntimeException(response.status.reason())
        }
      }
    }
  }

  override def update(path: File, configData: ConfigData, comment: String): Future[ConfigId] = {
    val entity = HttpEntity.IndefiniteLength(ContentTypes.`application/octet-stream`, configData.source)
    val formData: Multipart.FormData = FormData(FormData.BodyPart("conf", entity, Map("filename" → "")))
    val uri = Uri(location.uri.toString)
      .withPath(Path / "update")
      .withQuery(Query("path" → path.getPath, "comment" → comment))

    Marshal(formData).to[RequestEntity].flatMap { entity =>
      val request = HttpRequest(HttpMethods.POST, uri = uri, entity = entity)
      println(request)
      Http().singleRequest(request).flatMap { response ⇒
        response.status match {
          case StatusCodes.OK         ⇒ Unmarshal(response).to[ConfigId]
          case StatusCodes.BadRequest ⇒ throw new FileNotFoundException(response.status.reason())
          case _                      ⇒ throw new RuntimeException(response.status.reason())
        }
      }
    }
  }

  override def get(path: File, id: Option[ConfigId]): Future[Option[ConfigData]] = {
    val uri = Uri(location.uri.toString)
      .withPath(Path / "get")
      .withQuery(Query(Map("path" → path.getPath) ++ id.map(configId ⇒ "id" → configId.id.toString)))
    val request = HttpRequest(uri = uri)
    println(request)
    Http().singleRequest(request).map { response =>
      response.status match {
        case StatusCodes.OK       ⇒ Some(ConfigData(response.entity.dataBytes))
        case StatusCodes.NotFound ⇒ None
        case _                    ⇒ throw new RuntimeException(response.status.reason())
      }
    }
  }

  override def get(path: File, date: Date): Future[Option[ConfigData]] = {
    val uri = Uri(location.uri.toString)
      .withPath(Path / "get")
      .withQuery(Query(Map("path" → path.getPath, "date" → simpleDateFormat.format(date))))
    val request = HttpRequest(uri = uri)
    println(request)
    Http().singleRequest(request).map { response =>
      response.status match {
        case StatusCodes.OK       ⇒ Some(ConfigData(response.entity.dataBytes))
        case StatusCodes.NotFound ⇒ None
        case _                    ⇒ throw new RuntimeException(response.status.reason())
      }
    }
  }

  override def exists(path: File): Future[Boolean] = {
    val uri = Uri(location.uri.toString)
      .withPath(Path / "exists")
      .withQuery(Query(Map("path" → path.getPath)))
    val request = HttpRequest(uri = uri)
    println(request)
    Http().singleRequest(request).map { response =>
      response.status match {
        case StatusCodes.OK       ⇒ true
        case StatusCodes.NotFound ⇒ false
        case _                    ⇒ throw new RuntimeException(response.status.reason())
      }
    }
  }

  override def delete(path: File, comment: String): Future[Unit] = {
    val uri = Uri(location.uri.toString)
      .withPath(Path / "delete")
      .withQuery(Query(Map("path" → path.getPath, "comment" → comment)))
    val request = HttpRequest(HttpMethods.POST, uri = uri)
    println(request)
    Http().singleRequest(request).map { response =>
      response.status match {
        case StatusCodes.OK         ⇒ ()
        case StatusCodes.BadRequest ⇒ throw new FileNotFoundException(response.status.reason())
        case _                      ⇒ throw new RuntimeException(response.status.reason())
      }
    }
  }

  override def list(): Future[List[ConfigFileInfo]] = {
    val uri = Uri(location.uri.toString).withPath(Path / "list")
    val request = HttpRequest(uri = uri)
    println(request)
    Http().singleRequest(request).flatMap { response =>
      response.status match {
        case StatusCodes.OK ⇒ Unmarshal(response.entity).to[List[ConfigFileInfo]]
        case _              ⇒ throw new RuntimeException(response.status.reason())
      }
    }
  }

  override def history(path: File, maxResults: Int): Future[List[ConfigFileHistory]] = {
    val uri = Uri(location.uri.toString)
      .withPath(Path / "history")
      .withQuery(Query(Map("path" → path.getPath, "maxResults" → maxResults.toString)))
    val request = HttpRequest(uri = uri)
    println(request)
    Http().singleRequest(request).flatMap { response =>
      response.status match {
        case StatusCodes.OK ⇒ Unmarshal(response.entity).to[List[ConfigFileHistory]]
        case _              ⇒ throw new RuntimeException(response.status.reason())
      }
    }
  }

  override def setDefault(path: File, id: Option[ConfigId]): Future[Unit] = {
    val uri = Uri(location.uri.toString)
      .withPath(Path / "setDefault")
      .withQuery(Query(Map("path" → path.getPath) ++ id.map(configId ⇒ "id" → configId.id.toString)))
    val request = HttpRequest(HttpMethods.POST, uri = uri)
    println(request)
    Http().singleRequest(request).map { response =>
      response.status match {
        case StatusCodes.OK         ⇒ ()
        case StatusCodes.BadRequest ⇒ throw new FileNotFoundException(response.status.reason())
        case _                      ⇒ throw new RuntimeException(response.status.reason())
      }
    }
  }

  override def resetDefault(path: File): Future[Unit] = {
    val uri = Uri(location.uri.toString)
      .withPath(Path / "resetDefault")
      .withQuery(Query(Map("path" → path.getPath)))
    val request = HttpRequest(HttpMethods.POST, uri = uri)
    println(request)
    Http().singleRequest(request).map { response =>
      response.status match {
        case StatusCodes.OK         ⇒ ()
        case StatusCodes.BadRequest ⇒ throw new FileNotFoundException(response.status.reason())
        case _                      ⇒ throw new RuntimeException(response.status.reason())
      }
    }
  }

  override def getDefault(path: File): Future[Option[ConfigData]] = {
    val uri = Uri(location.uri.toString)
      .withPath(Path / "getDefault")
      .withQuery(Query(Map("path" → path.getPath)))
    val request = HttpRequest(uri = uri)
    println(request)
    Http().singleRequest(request).map { response =>
      response.status match {
        case StatusCodes.OK       ⇒ Some(ConfigData(response.entity.dataBytes))
        case StatusCodes.NotFound ⇒ None
        case _                    ⇒ throw new RuntimeException(response.status.reason())
      }
    }
  }
}
