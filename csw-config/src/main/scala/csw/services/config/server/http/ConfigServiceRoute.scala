package csw.services.config.server.http

import java.io.{FileNotFoundException, IOException}

import akka.Done
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import csw.services.config.api.scaladsl.ConfigService
import csw.services.config.server.ActorRuntime
import csw.services.config.api.exceptions.{FileNotFound, FileAlreadyExists}

import scala.util.control.NonFatal

class ConfigServiceRoute(configService: ConfigService, actorRuntime: ActorRuntime) extends HttpSupport {

  import actorRuntime._

  def route: Route = handleExceptions(exceptionHandler) {
    path("config" / FilePath) { filePath ⇒
      (get & rejectEmptyResponse) {
        (dateParam & defaultParam & idParam) {
          case (Some(date), _, _) ⇒ complete(configService.get(filePath, date))
          case (_, true, _)       ⇒ complete(configService.getDefault(filePath))
          case (_, _, maybeId)    ⇒ complete(configService.get(filePath, maybeId))
        }
      } ~
        (head & idParam) { id ⇒
          complete {
            configService.exists(filePath, id).map { found ⇒
              if (found) StatusCodes.OK else StatusCodes.NotFound
            }
          }
        } ~
        post {
          (configDataEntity & oversizeParam & commentParam) { (configData, oversize, comment) ⇒
            complete(StatusCodes.Created -> configService.create(filePath, configData, oversize, comment))
          }
        } ~
        put {
          (configDataEntity & commentParam) { (configData, comment) ⇒
            complete(configService.update(filePath, configData, comment))
          }
        } ~
        delete {
          commentParam { comment ⇒
            complete(configService.delete(filePath, comment).map(_ ⇒ Done))
          }
        }
    } ~
      (path("default" / FilePath) & put) { filePath ⇒
        idParam { maybeId ⇒
          complete(configService.setDefault(filePath, maybeId).map(_ ⇒ Done))
        }
      } ~
      (path("history" / FilePath) & get) { filePath ⇒
        maxResultsParam { maxCount ⇒
          complete(configService.history(filePath, maxCount))
        }
      } ~
      (path("list") & get) {
        complete(configService.list())
      }
  }

  private val exceptionHandler = ExceptionHandler {
    case ex: FileAlreadyExists ⇒
      ex.printStackTrace()
      complete(HttpResponse(StatusCodes.Conflict, entity = ex.getMessage))
    case ex: FileNotFound      ⇒
      ex.printStackTrace()
      complete(HttpResponse(StatusCodes.NotFound, entity = ex.getMessage))
    case NonFatal(ex)          ⇒
      ex.printStackTrace()
      complete(HttpResponse(StatusCodes.InternalServerError, entity = ex.getMessage))
  }
}
