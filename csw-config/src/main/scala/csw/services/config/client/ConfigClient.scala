package csw.services.config.client

import java.io.{FileNotFoundException, IOException}
import java.nio.{file => jnio}
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

import scala.concurrent.Future

class ConfigClient(location: Location, actorRuntime: ActorRuntime) extends ConfigManager with JsonSupport {

  import actorRuntime._

  override def name: String = "http-based-config-client"

  override def create(path: jnio.Path, configData: ConfigData, oversize: Boolean, comment: String): Future[ConfigId] = {
    val entity = HttpEntity.IndefiniteLength(ContentTypes.`application/octet-stream`, configData.source)
    val formData: Multipart.FormData = FormData(FormData.BodyPart("conf", entity, Map("filename" → "")))
    val uri = Uri(location.uri.toString)
      .withPath(Path / "create")
      .withQuery(Query("path" → path.toString, "oversize" → oversize.toString, "comment" → comment))

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

  override def update(path: jnio.Path, configData: ConfigData, comment: String): Future[ConfigId] = {
    val entity = HttpEntity.IndefiniteLength(ContentTypes.`application/octet-stream`, configData.source)
    val formData: Multipart.FormData = FormData(FormData.BodyPart("conf", entity, Map("filename" → "")))
    val uri = Uri(location.uri.toString)
      .withPath(Path / "update")
      .withQuery(Query("path" → path.toString, "comment" → comment))

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

  override def get(path: jnio.Path, id: Option[ConfigId]): Future[Option[ConfigData]] = {
    val uri = Uri(location.uri.toString)
      .withPath(Path / "get")
      .withQuery(Query(Map("path" → path.toString) ++ id.map(configId ⇒ "id" → configId.id.toString)))
    val request = HttpRequest(uri = uri)
    println(request)
    Http().singleRequest(request).map { response =>
      response.status match {
        case StatusCodes.OK       ⇒ Some(ConfigData.fromSource(response.entity.dataBytes))
        case StatusCodes.NotFound ⇒ None
        case _                    ⇒ throw new RuntimeException(response.status.reason())
      }
    }
  }

  override def get(path: jnio.Path, date: Date): Future[Option[ConfigData]] = {
    val uri = Uri(location.uri.toString)
      .withPath(Path / "get")
      .withQuery(Query("path" → path.toString, "date" → simpleDateFormat.format(date)))
    val request = HttpRequest(uri = uri)
    println(request)
    Http().singleRequest(request).map { response =>
      response.status match {
        case StatusCodes.OK       ⇒ Some(ConfigData.fromSource(response.entity.dataBytes))
        case StatusCodes.NotFound ⇒ None
        case _                    ⇒ throw new RuntimeException(response.status.reason())
      }
    }
  }

  override def exists(path: jnio.Path): Future[Boolean] = {
    val uri = Uri(location.uri.toString)
      .withPath(Path / "exists")
      .withQuery(Query("path" → path.toString))
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

  override def delete(path: jnio.Path, comment: String): Future[Unit] = {
    val uri = Uri(location.uri.toString)
      .withPath(Path / "delete")
      .withQuery(Query("path" → path.toString, "comment" → comment))
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

  override def history(path: jnio.Path, maxResults: Int): Future[List[ConfigFileHistory]] = {
    val uri = Uri(location.uri.toString)
      .withPath(Path / "history")
      .withQuery(Query("path" → path.toString, "maxResults" → maxResults.toString))
    val request = HttpRequest(uri = uri)
    println(request)
    Http().singleRequest(request).flatMap { response =>
      response.status match {
        case StatusCodes.OK ⇒ Unmarshal(response.entity).to[List[ConfigFileHistory]]
        case _              ⇒ throw new RuntimeException(response.status.reason())
      }
    }
  }

  override def setDefault(path: jnio.Path, id: Option[ConfigId]): Future[Unit] = {
    val uri = Uri(location.uri.toString)
      .withPath(Path / "setDefault")
      .withQuery(Query(Map("path" → path.toString) ++ id.map(configId ⇒ "id" → configId.id.toString)))
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

  override def resetDefault(path: jnio.Path): Future[Unit] = {
    val uri = Uri(location.uri.toString)
      .withPath(Path / "resetDefault")
      .withQuery(Query("path" → path.toString))
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

  override def getDefault(path: jnio.Path): Future[Option[ConfigData]] = {
    val uri = Uri(location.uri.toString)
      .withPath(Path / "getDefault")
      .withQuery(Query("path" → path.toString))
    val request = HttpRequest(uri = uri)
    println(request)
    Http().singleRequest(request).map { response =>
      response.status match {
        case StatusCodes.OK       ⇒ Some(ConfigData.fromSource(response.entity.dataBytes))
        case StatusCodes.NotFound ⇒ None
        case _                    ⇒ throw new RuntimeException(response.status.reason())
      }
    }
  }
}
