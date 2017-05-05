package csw.services.config.server.http

import akka.Done
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import csw.services.config.api.scaladsl.ConfigService
import csw.services.config.server.ActorRuntime

class ConfigServiceRoute(
    configService: ConfigService,
    actorRuntime: ActorRuntime,
    configExceptionHandler: ConfigExceptionHandler
) extends HttpSupport {

  import actorRuntime._

  def route: Route = handleExceptions(configExceptionHandler.exceptionHandler) {
    path("config" / FilePath) { filePath ⇒
      (get & rejectEmptyResponse) {
        (dateParam & idParam) {
          case (Some(date), _) ⇒ complete(configService.getByTime(filePath, date))
          case (_, Some(id))   ⇒ complete(configService.getById(filePath, id))
          case (_, _)          ⇒ complete(configService.getLatest(filePath))
        }
      } ~
      head {
        idParam { id ⇒
          complete {
            configService.exists(filePath, id).map { found ⇒
              if (found) StatusCodes.OK else StatusCodes.NotFound
            }
          }
        }
      } ~
      post {
        (configDataEntity & annexParam & commentParam) { (configData, annex, comment) ⇒
          complete(StatusCodes.Created -> configService.create(filePath, configData, annex, comment))
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
    (path("active-config" / FilePath) & get & rejectEmptyResponse) { filePath ⇒
      println(s"------------------------getting file for active version of $filePath -----------------------")
      dateParam {
        case Some(date) ⇒ complete(configService.getActiveByTime(filePath, date))
        case _          ⇒ complete(configService.getActive(filePath))
      }
    } ~
    path("active-version" / FilePath) { filePath ⇒
      put {
        (idParam & commentParam) {
          case (Some(configId), comment) ⇒
            complete(configService.setActiveVersion(filePath, configId, comment).map(_ ⇒ Done))
          case (_, comment) ⇒ complete(configService.resetActiveVersion(filePath, comment).map(_ ⇒ Done))
        }
      } ~
      (get & rejectEmptyResponse) {
        println(s"------------------------getting active version of $filePath -----------------------")
        complete(configService.getActiveVersion(filePath))
      }
    } ~
    (path("history" / FilePath) & get) { filePath ⇒
      maxResultsParam { maxCount ⇒
        complete(configService.history(filePath, maxCount))
      }
    } ~
    (path("list") & get) {
      (typeParam & patternParam) { (fileType, pattern) ⇒
        complete(configService.list(fileType, pattern))
      }
    }
  }
}
