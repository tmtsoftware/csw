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
}
