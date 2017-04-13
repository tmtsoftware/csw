package csw.services.config.server.http

import akka.Done
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import csw.services.config.api.scaladsl.ConfigService
import csw.services.config.server.ActorRuntime

import scala.util.control.NonFatal

class ConfigServiceRoute(configManager: ConfigService, actorRuntime: ActorRuntime) extends HttpSupport {

  import actorRuntime._

  def route: Route = handleExceptions(exceptionHandler) {
    get {
      path("get") {
        (pathParam & dateParam) { (filePath, date) ⇒
          rejectEmptyResponse & complete {
            configManager.get(filePath, date)
          }
        }
      } ~
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
                if (found) Some("convert me into a head request") else None
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
          } ~
          path("delete") {
            (pathParam & commentParam) { (filePath, comment) ⇒
              complete(configManager.delete(filePath, comment).map(_ ⇒ Done))
            }
          }
      }
  }

  private val exceptionHandler = ExceptionHandler {
    case ClientException(ex) ⇒
      ex.printStackTrace()
      complete(HttpResponse(StatusCodes.BadRequest, entity = ex.getMessage))
    case NonFatal(ex)        ⇒
      ex.printStackTrace()
      complete(HttpResponse(StatusCodes.InternalServerError, entity = ex.getMessage))
  }
}
