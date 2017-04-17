package csw.services.config.client.internal

import java.nio.{file ⇒ jnio}
import java.time.Instant

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpEntity.Chunked
import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import csw.services.config.api.exceptions.{FileAlreadyExists, FileNotFound}
import csw.services.config.api.models._
import csw.services.config.api.scaladsl.ConfigService
import csw.services.config.server.http.JsonSupport

import scala.async.Async._
import scala.concurrent.Future

class ConfigClient(configServiceResolver: ConfigServiceResolver, actorRuntime: ActorRuntime) extends ConfigService with JsonSupport {

  import actorRuntime._

  override def name: String = "http-based-config-client"

  private def configUri(path: jnio.Path) = baseUri(Path / "config" ++ Path / Path(path.toString))
  private def defaultUri(path: jnio.Path) = baseUri(Path / "default" ++ Path / Path(path.toString))
  private def historyUri(path: jnio.Path) = baseUri(Path / "history" ++ Path / Path(path.toString))

  private def listUri = baseUri(Path / "list")

  private def baseUri(path: Path) = async {
    await(configServiceResolver.uri).withPath(path)
  }

  override def create(path: jnio.Path, configData: ConfigData, oversize: Boolean, comment: String): Future[ConfigId] = async {
    val entity = Chunked.fromData(ContentTypes.`application/octet-stream`, configData.source)
    val uri = await(configUri(path)).withQuery(Query("oversize" → oversize.toString, "comment" → comment))

    val request = HttpRequest(HttpMethods.POST, uri = uri, entity = entity)
    println(request)
    val response = await(Http().singleRequest(request))

    response.status match {
      case StatusCodes.Created  ⇒ await(Unmarshal(response).to[ConfigId])
      case StatusCodes.Conflict ⇒ throw FileAlreadyExists(path)
      case _                    ⇒ throw new RuntimeException(response.status.reason())
    }
  }

  override def update(path: jnio.Path, configData: ConfigData, comment: String): Future[ConfigId] = async {
    val entity = Chunked.fromData(ContentTypes.`application/octet-stream`, configData.source)
    val uri = await(configUri(path)).withQuery(Query("comment" → comment))

    val request = HttpRequest(HttpMethods.PUT, uri = uri, entity = entity)
    println(request)
    val response = await(Http().singleRequest(request))

    response.status match {
      case StatusCodes.OK       ⇒ await(Unmarshal(response).to[ConfigId])
      case StatusCodes.NotFound ⇒ throw FileNotFound(path)
      case _                    ⇒ throw new RuntimeException(response.status.reason())
    }
  }

  override def get(path: jnio.Path, id: Option[ConfigId]): Future[Option[ConfigData]] = async {
    val uri = await(configUri(path)).withQuery(Query(id.map(configId ⇒ "id" → configId.id.toString).toMap))

    val request = HttpRequest(uri = uri)
    println(request)
    val response = await(Http().singleRequest(request))

    response.status match {
      case StatusCodes.OK       ⇒ Some(ConfigData.fromSource(response.entity.dataBytes))
      case StatusCodes.NotFound ⇒ None
      case _                    ⇒ throw new RuntimeException(response.status.reason())
    }
  }

  override def get(path: jnio.Path, time: Instant): Future[Option[ConfigData]] = async {
    val uri = await(configUri(path)).withQuery(Query("date" → time.toString))

    val request = HttpRequest(uri = uri)
    println(request)
    val response = await(Http().singleRequest(request))

    response.status match {
      case StatusCodes.OK       ⇒ Some(ConfigData.fromSource(response.entity.dataBytes))
      case StatusCodes.NotFound ⇒ None
      case _                    ⇒ throw new RuntimeException(response.status.reason())
    }
  }

  override def exists(path: jnio.Path, id: Option[ConfigId]): Future[Boolean] = async {
    val uri = await(configUri(path)).withQuery(Query(id.map(configId ⇒ "id" → configId.id.toString).toMap))

    val request = HttpRequest(HttpMethods.HEAD, uri = uri)
    println(request)
    val response = await(Http().singleRequest(request))

    response.status match {
      case StatusCodes.OK       ⇒ true
      case StatusCodes.NotFound ⇒ false
      case _                    ⇒ throw new RuntimeException(response.status.reason())
    }
  }

  override def delete(path: jnio.Path, comment: String): Future[Unit] = async {
    val uri = await(configUri(path)).withQuery(Query("comment" → comment))

    val request = HttpRequest(HttpMethods.DELETE, uri = uri)
    println(request)
    val response = await(Http().singleRequest(request))

    response.status match {
      case StatusCodes.OK       ⇒ ()
      case StatusCodes.NotFound ⇒ throw FileNotFound(path)
      case _                    ⇒ throw new RuntimeException(response.status.reason())
    }
  }

  override def list(): Future[List[ConfigFileInfo]] = async {
    val uri = await(listUri)

    val request = HttpRequest(uri = uri)
    println(request)
    val response = await(Http().singleRequest(request))

    response.status match {
      case StatusCodes.OK ⇒ await(Unmarshal(response.entity).to[List[ConfigFileInfo]])
      case _              ⇒ throw new RuntimeException(response.status.reason())
    }
  }

  override def history(path: jnio.Path, maxResults: Int): Future[List[ConfigFileHistory]] = async {
    val uri = await(historyUri(path)).withQuery(Query("maxResults" → maxResults.toString))

    val request = HttpRequest(uri = uri)
    println(request)
    val response = await(Http().singleRequest(request))

    response.status match {
      case StatusCodes.OK       ⇒ await(Unmarshal(response.entity).to[List[ConfigFileHistory]])
      case StatusCodes.NotFound ⇒ throw FileNotFound(path)
      case _                    ⇒ throw new RuntimeException(response.status.reason())
    }
  }

  override def setDefault(path: jnio.Path, id: Option[ConfigId]): Future[Unit] = async {
    val uri = await(defaultUri(path)).withQuery(Query(id.map(configId ⇒ "id" → configId.id.toString).toMap))

    val request = HttpRequest(HttpMethods.PUT, uri = uri)
    println(request)
    val response = await(Http().singleRequest(request))

    response.status match {
      case StatusCodes.OK       ⇒ ()
      case StatusCodes.NotFound ⇒ throw FileNotFound(path)
      case _                    ⇒ throw new RuntimeException(response.status.reason())
    }
  }

  override def getDefault(path: jnio.Path): Future[Option[ConfigData]] = async {
    val uri = await(configUri(path)).withQuery(Query("default" → "true"))

    val request = HttpRequest(uri = uri)
    println(request)
    val response = await(Http().singleRequest(request))

    response.status match {
      case StatusCodes.OK       ⇒ Some(ConfigData.fromSource(response.entity.dataBytes))
      case StatusCodes.NotFound ⇒ None
      case _                    ⇒ throw new RuntimeException(response.status.reason())
    }
  }
}
