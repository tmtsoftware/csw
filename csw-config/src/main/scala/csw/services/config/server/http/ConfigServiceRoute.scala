package csw.services.config.server.http

import akka.Done
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import csw.services.config.api.commons.ActorRuntime
import csw.services.config.api.scaladsl.ConfigManager

import scala.util.control.NonFatal

class ConfigServiceRoute(configManager: ConfigManager, actorRuntime: ActorRuntime) extends HttpSupport {

  import actorRuntime._

  def route: Route = handleExceptions(exceptionHandler) {
    get {
      path("get") {
        (pathParam & idParam) { (filePath, maybeConfigId) ⇒
          rejectEmptyResponse & complete {
            configManager.get(filePath, maybeConfigId)
          }
        }
      } ~
        path("getDefault") {
          pathParam { filePath ⇒
            rejectEmptyResponse & complete {
              configManager.getDefault(filePath)
            }
          }
        } ~
        path("exists") {
          pathParam { filePath ⇒
            rejectEmptyResponse & complete {
              configManager.exists(filePath).map { found ⇒
                if (found) Some(Done) else None
              }
            }
          }
        } ~
        path("list") {
          complete(configManager.list())
        } ~
        path("history") {
          (pathParam & maxResultsParam) { (filePath, maxCount) ⇒
            complete(configManager.history(filePath, maxCount))
          }
        }
    } ~
      post {
        path("create") {
          (pathParam & fileDataParam & oversizeParam & commentParam) { (filePath, configSource, oversize, comment) ⇒
            complete(configManager.create(filePath, configSource, oversize, comment))
          }
        } ~
          path("update") {
            (pathParam & fileDataParam & commentParam) { (filePath, configSource, comment) ⇒
              complete(configManager.update(filePath, configSource, comment))
            }
          } ~
          path("setDefault") {
            (pathParam & idParam) { (filePath, maybeConfigId) ⇒
              complete(configManager.setDefault(filePath, maybeConfigId).map(_ ⇒ Done))
            }
          } ~
          path("resetDefault") {
            pathParam { filePath ⇒
              complete(configManager.resetDefault(filePath).map(_ ⇒ Done))
            }
          }
      }
  }

  private val exceptionHandler = ExceptionHandler {
    case ClientException(ex) ⇒ complete(HttpResponse(StatusCodes.BadRequest, entity = ex.getMessage))
    case NonFatal(ex)        ⇒ complete(HttpResponse(StatusCodes.InternalServerError, entity = ex.getMessage))
  }
}
