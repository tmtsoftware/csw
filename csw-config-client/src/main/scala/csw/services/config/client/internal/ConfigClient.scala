package csw.services.config.client.internal

import java.nio.{file ⇒ jnio}
import java.time.Instant

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import csw.services.config.api.commons.ConfigStreamExts.RichSource
import csw.services.config.api.commons.{BinaryUtils, FileType}
import csw.services.config.api.exceptions.{FileAlreadyExists, FileNotFound, InvalidInput}
import csw.services.config.api.models.{JsonSupport, _}
import csw.services.config.api.scaladsl.ConfigService

import scala.async.Async._
import scala.concurrent.Future

class ConfigClient(configServiceResolver: ConfigServiceResolver, actorRuntime: ActorRuntime)
    extends ConfigService
    with JsonSupport {

  import actorRuntime._

  private def configUri(path: jnio.Path): Future[Uri] = baseUri(Path / "config" ++ Path / Path(path.toString))
  private def activeConfig(path: jnio.Path)           = baseUri(Path / "active-config" ++ Path / Path(path.toString))
  private def activeConfigVersion(path: jnio.Path)    = baseUri(Path / "active-version" ++ Path / Path(path.toString))
  private def historyUri(path: jnio.Path)             = baseUri(Path / "history" ++ Path / Path(path.toString))
  private def metadataUri                             = baseUri(Path / "metadata")

  private def listUri = baseUri(Path / "list")

  private def baseUri(path: Path) = async {
    await(configServiceResolver.uri).withPath(path)
  }

  override def create(path: jnio.Path, configData: ConfigData, annex: Boolean, comment: String): Future[ConfigId] =
    async {
      val (prefix, stitchedSource) = configData.source.prefixAndStitch(1)
      val isAnnex                  = if (annex) annex else BinaryUtils.isBinary(await(prefix))
      val uri                      = await(configUri(path)).withQuery(Query("annex" → isAnnex.toString, "comment" → comment))
      val entity                   = HttpEntity(ContentTypes.`application/octet-stream`, configData.length, stitchedSource)

      val request = HttpRequest(HttpMethods.POST, uri = uri, entity = entity)
      println(request)
      val response = await(Http().singleRequest(request))

      await(
        handleResponse(response) {
          case StatusCodes.Created  ⇒ Unmarshal(response).to[ConfigId]
          case StatusCodes.Conflict ⇒ throw FileAlreadyExists(path)
        }
      )
    }

  override def update(path: jnio.Path, configData: ConfigData, comment: String): Future[ConfigId] = async {
    val entity = HttpEntity(ContentTypes.`application/octet-stream`, configData.length, configData.source)
    val uri    = await(configUri(path)).withQuery(Query("comment" → comment))

    val request = HttpRequest(HttpMethods.PUT, uri = uri, entity = entity)
    println(request)
    val response = await(Http().singleRequest(request))

    await(
      handleResponse(response) {
        case StatusCodes.OK ⇒ Unmarshal(response).to[ConfigId]
      }
    )
  }

  override def getActive(path: jnio.Path): Future[Option[ConfigData]] = async {
    await(get(await(activeConfig(path))))
  }

  override def getLatest(path: jnio.Path): Future[Option[ConfigData]] = async {
    await(get(await(configUri(path))))
  }

  override def getById(path: jnio.Path, configId: ConfigId): Future[Option[ConfigData]] = async {
    await(get(await(configUri(path)).withQuery(Query("id" → configId.id))))
  }

  override def getByTime(path: jnio.Path, time: Instant): Future[Option[ConfigData]] = async {
    await(get(await(configUri(path)).withQuery(Query("date" → time.toString))))
  }

  override def exists(path: jnio.Path, id: Option[ConfigId]): Future[Boolean] = async {
    val uri = await(configUri(path)).withQuery(Query(id.map(configId ⇒ "id" → configId.id.toString).toMap))

    val request = HttpRequest(HttpMethods.HEAD, uri = uri)
    println(request)
    val response = await(Http().singleRequest(request))

    await(
      handleResponse(response) {
        case StatusCodes.OK       ⇒ Future.successful(true)
        case StatusCodes.NotFound ⇒ Future.successful(false)
      }
    )
  }

  override def delete(path: jnio.Path, comment: String): Future[Unit] = async {
    val uri = await(configUri(path)).withQuery(Query("comment" → comment))

    val request = HttpRequest(HttpMethods.DELETE, uri = uri)
    println(request)
    val response = await(Http().singleRequest(request))

    await(
      handleResponse(response) {
        case StatusCodes.OK ⇒ Future.unit
      }
    )
  }

  override def list(fileType: Option[FileType] = None, pattern: Option[String] = None): Future[List[ConfigFileInfo]] =
    async {
      val uri =
        await(listUri).withQuery(Query(fileType.map("type" → _.entryName).toMap ++ pattern.map("pattern" → _).toMap))

      val request = HttpRequest(uri = uri)
      println(request)
      val response = await(Http().singleRequest(request))

      await(
        handleResponse(response) {
          case StatusCodes.OK ⇒ Unmarshal(response).to[List[ConfigFileInfo]]
        }
      )

    }

  override def history(path: jnio.Path, maxResults: Int): Future[List[ConfigFileRevision]] = async {
    val uri = await(historyUri(path)).withQuery(Query("maxResults" → maxResults.toString))

    val request = HttpRequest(uri = uri)
    println(request)
    val response = await(Http().singleRequest(request))

    await(
      handleResponse(response) {
        case StatusCodes.OK ⇒ Unmarshal(response).to[List[ConfigFileRevision]]
      }
    )
  }

  override def setActiveVersion(path: jnio.Path, id: ConfigId, comment: String): Future[Unit] =
    handleActiveConfig(path, Query("id" → id.id.toString, "comment" → comment))

  override def resetActiveVersion(path: jnio.Path, comment: String): Future[Unit] =
    handleActiveConfig(path, Query.Empty)

  override def getActiveByTime(path: jnio.Path, time: Instant): Future[Option[ConfigData]] = async {
    await(get(await(activeConfig(path)).withQuery(Query("date" → time.toString))))
  }

  override def getActiveVersion(path: jnio.Path): Future[ConfigId] = async {
    val uri = await(activeConfigVersion(path))

    val request = HttpRequest(uri = uri)
    println(request)
    val response = await(Http().singleRequest(request))

    val lengthOption = response.entity.contentLengthOption

    await(
      handleResponse(response) {
        case StatusCodes.OK ⇒ Unmarshal(response).to[ConfigId]
      }
    )
  }

  private def handleActiveConfig(path: jnio.Path, query: Query): Future[Unit] = async {
    val uri = await(activeConfigVersion(path)).withQuery(query)

    val request = HttpRequest(HttpMethods.PUT, uri = uri)
    println(request)
    val response = await(Http().singleRequest(request))

    await(
      handleResponse(response) {
        case StatusCodes.OK ⇒ Future.unit
      }
    )
  }

  private def get(uri: Uri): Future[Option[ConfigData]] = async {
    val request = HttpRequest(uri = uri)
    println(request)
    val response = await(Http().singleRequest(request))

    val lengthOption = response.entity.contentLengthOption

    await(
      handleResponse(response) {
        case StatusCodes.OK if lengthOption.isDefined ⇒
          Future.successful(Some(ConfigData.from(response.entity.dataBytes, lengthOption.get)))
        case StatusCodes.OK ⇒
          //Not consuming the file content will block the connection.
          response.entity.discardBytes()
          throw new RuntimeException("response must have content-length")
        case StatusCodes.NotFound ⇒ Future.successful(None)
      }
    )
  }

  def handleResponse[T](response: HttpResponse)(pf: PartialFunction[StatusCode, Future[T]]): Future[T] = {
    def contentF = Unmarshal(response).to[String]

    val defaultHandler: PartialFunction[StatusCode, Future[T]] = {
      case StatusCodes.BadRequest ⇒ contentF.map(message ⇒ throw InvalidInput(message))
      case StatusCodes.NotFound   ⇒ contentF.map(message ⇒ throw FileNotFound(message))
      case _                      ⇒ contentF.map(message ⇒ throw new RuntimeException(message))
    }

    val handler = pf.orElse(defaultHandler)
    handler(response.status)
  }

  override def getMetadata: Future[ConfigMetadata] = async {
    val request = HttpRequest(uri = await(metadataUri))
    println(request)
    val response = await(Http().singleRequest(request))

    response.status match {
      case StatusCodes.OK         ⇒ await(Unmarshal(response.entity).to[ConfigMetadata])
      case StatusCodes.BadRequest ⇒ throw InvalidInput(await(Unmarshal(response).to[String]))
      case _                      ⇒ throw new RuntimeException(await(Unmarshal(response).to[String]))
    }
  }
}
