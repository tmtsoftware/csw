package csw.services.config.server.http

import akka.Done
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import csw.services.config.api.scaladsl.ConfigService
import csw.services.config.server.ActorRuntime

import scala.util.control.NonFatal

class ConfigServiceRoute(configService: ConfigService, actorRuntime: ActorRuntime) extends HttpSupport {

  import actorRuntime._

  def route: Route = handleExceptions(exceptionHandler) {
    get {
      path("get") {
        (pathParam & dateParam) { (filePath, date) ⇒
          rejectEmptyResponse & complete {
            configService.get(filePath, date)
          }
        }
      } ~
        path("get") {
          (pathParam & idParam) { (filePath, maybeConfigId) ⇒
            rejectEmptyResponse & complete {
              configService.get(filePath, maybeConfigId)
            }
          }
        } ~
        path("getDefault") {
          pathParam { filePath ⇒
            rejectEmptyResponse & complete {
              configService.getDefault(filePath)
            }
          }
        } ~
        path("exists") {
          pathParam { filePath ⇒
            rejectEmptyResponse & complete {
              configService.exists(filePath).map { found ⇒
                if (found) Some("convert me into a head request") else None
              }
            }
          }
        } ~
        path("list") {
          complete(configService.list())
        } ~
        path("history") {
          (pathParam & maxResultsParam) { (filePath, maxCount) ⇒
            complete(configService.history(filePath, maxCount))
          }
        }
    } ~
      post {
        path("create") {
          (pathParam & configDataEntity & oversizeParam & commentParam) { (filePath, configData, oversize, comment) ⇒
            complete(configService.create(filePath, configData, oversize, comment))
          }
        } ~
          path("update") {
            (pathParam & configDataEntity & commentParam) { (filePath, configData, comment) ⇒
              complete(configService.update(filePath, configData, comment))
            }
          } ~
          path("setDefault") {
            (pathParam & idParam) { (filePath, maybeConfigId) ⇒
              complete(configService.setDefault(filePath, maybeConfigId).map(_ ⇒ Done))
            }
          } ~
          path("resetDefault") {
            pathParam { filePath ⇒
              complete(configService.resetDefault(filePath).map(_ ⇒ Done))
            }
          } ~
          path("delete") {
            (pathParam & commentParam) { (filePath, comment) ⇒
              complete(configService.delete(filePath, comment).map(_ ⇒ Done))
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
