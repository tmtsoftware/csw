package csw.services.config.client.internal

import java.io.{FileNotFoundException, IOException}
import java.nio.{file ⇒ jnio}
import java.time.Instant

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpEntity.Chunked
import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import csw.services.config.api.javadsl.IConfigService
import csw.services.config.api.models._
import csw.services.config.api.scaladsl.ConfigService
import csw.services.config.server.http.JsonSupport

import scala.async.Async._
import scala.concurrent.Future

class ConfigClient(configServiceResolver: ConfigServiceResolver, actorRuntime: ActorRuntime) extends ConfigService with JsonSupport {

  import actorRuntime._

  override def name: String = "http-based-config-client"

  override def create(path: jnio.Path, configData: ConfigData, oversize: Boolean, comment: String): Future[ConfigId] = async {
    val entity = Chunked.fromData(ContentTypes.`application/octet-stream`, configData.source)
    val uri = await(configServiceResolver.uri)
      .withPath(Path / "create")
      .withQuery(Query("path" → path.toString, "oversize" → oversize.toString, "comment" → comment))

    val request = HttpRequest(HttpMethods.POST, uri = uri, entity = entity)
    println(request)
    val response = await(Http().singleRequest(request))

    response.status match {
      case StatusCodes.OK         ⇒ await(Unmarshal(response).to[ConfigId])
      case StatusCodes.BadRequest ⇒ throw new IOException(response.status.reason())
      case _                      ⇒ throw new RuntimeException(response.status.reason())
    }
  }

  override def update(path: jnio.Path, configData: ConfigData, comment: String): Future[ConfigId] = async {
    val entity = Chunked.fromData(ContentTypes.`application/octet-stream`, configData.source)
    val uri = await(configServiceResolver.uri)
      .withPath(Path / "update")
      .withQuery(Query("path" → path.toString, "comment" → comment))

    val request = HttpRequest(HttpMethods.POST, uri = uri, entity = entity)
    println(request)
    val response = await(Http().singleRequest(request))

    response.status match {
      case StatusCodes.OK         ⇒ await(Unmarshal(response).to[ConfigId])
      case StatusCodes.BadRequest ⇒ throw new FileNotFoundException(response.status.reason())
      case _                      ⇒ throw new RuntimeException(response.status.reason())
    }
  }

  override def get(path: jnio.Path, id: Option[ConfigId]): Future[Option[ConfigData]] = async {
    val uri = await(configServiceResolver.uri)
      .withPath(Path / "get")
      .withQuery(Query(Map("path" → path.toString) ++ id.map(configId ⇒ "id" → configId.id.toString)))

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
    val uri = await(configServiceResolver.uri)
      .withPath(Path / "get")
      .withQuery(Query("path" → path.toString, "date" → time.toString))

    val request = HttpRequest(uri = uri)
    println(request)
    val response = await(Http().singleRequest(request))

    response.status match {
      case StatusCodes.OK       ⇒ Some(ConfigData.fromSource(response.entity.dataBytes))
      case StatusCodes.NotFound ⇒ None
      case _                    ⇒ throw new RuntimeException(response.status.reason())
    }
  }

  override def exists(path: jnio.Path): Future[Boolean] = async {
    val uri = await(configServiceResolver.uri)
      .withPath(Path / "exists")
      .withQuery(Query("path" → path.toString))

    val request = HttpRequest(uri = uri)
    println(request)
    val response = await(Http().singleRequest(request))

    response.status match {
      case StatusCodes.OK       ⇒ true
      case StatusCodes.NotFound ⇒ false
      case _                    ⇒ throw new RuntimeException(response.status.reason())
    }
  }

  override def delete(path: jnio.Path, comment: String): Future[Unit] = async {
    val uri = await(configServiceResolver.uri)
      .withPath(Path / "delete")
      .withQuery(Query("path" → path.toString, "comment" → comment))

    val request = HttpRequest(HttpMethods.POST, uri = uri)
    println(request)
    val response = await(Http().singleRequest(request))

    response.status match {
      case StatusCodes.OK         ⇒ ()
      case StatusCodes.BadRequest ⇒ throw new FileNotFoundException(response.status.reason())
      case _                      ⇒ throw new RuntimeException(response.status.reason())
    }
  }

  override def list(): Future[List[ConfigFileInfo]] = async {
    val uri = await(configServiceResolver.uri).withPath(Path / "list")

    val request = HttpRequest(uri = uri)
    println(request)
    val response = await(Http().singleRequest(request))

    response.status match {
      case StatusCodes.OK ⇒ await(Unmarshal(response.entity).to[List[ConfigFileInfo]])
      case _              ⇒ throw new RuntimeException(response.status.reason())
    }
  }

  override def history(path: jnio.Path, maxResults: Int): Future[List[ConfigFileHistory]] = async {
    val uri = await(configServiceResolver.uri)
      .withPath(Path / "history")
      .withQuery(Query("path" → path.toString, "maxResults" → maxResults.toString))

    val request = HttpRequest(uri = uri)
    println(request)
    val response = await(Http().singleRequest(request))

    response.status match {
      case StatusCodes.OK ⇒ await(Unmarshal(response.entity).to[List[ConfigFileHistory]])
      case _              ⇒ throw new RuntimeException(response.status.reason())
    }
  }

  override def setDefault(path: jnio.Path, id: Option[ConfigId]): Future[Unit] = async {
    val uri = await(configServiceResolver.uri)
      .withPath(Path / "setDefault")
      .withQuery(Query(Map("path" → path.toString) ++ id.map(configId ⇒ "id" → configId.id.toString)))

    val request = HttpRequest(HttpMethods.POST, uri = uri)
    println(request)
    val response = await(Http().singleRequest(request))

    response.status match {
      case StatusCodes.OK         ⇒ ()
      case StatusCodes.BadRequest ⇒ throw new FileNotFoundException(response.status.reason())
      case _                      ⇒ throw new RuntimeException(response.status.reason())
    }
  }

  override def resetDefault(path: jnio.Path): Future[Unit] = async {
    val uri = await(configServiceResolver.uri)
      .withPath(Path / "resetDefault")
      .withQuery(Query("path" → path.toString))

    val request = HttpRequest(HttpMethods.POST, uri = uri)
    println(request)
    val response = await(Http().singleRequest(request))

    response.status match {
      case StatusCodes.OK         ⇒ ()
      case StatusCodes.BadRequest ⇒ throw new FileNotFoundException(response.status.reason())
      case _                      ⇒ throw new RuntimeException(response.status.reason())
    }
  }

  override def getDefault(path: jnio.Path): Future[Option[ConfigData]] = async {
    val uri = await(configServiceResolver.uri)
      .withPath(Path / "getDefault")
      .withQuery(Query("path" → path.toString))

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
