package csw.services.config.server.http

import akka.Done
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import csw.services.config.api.scaladsl.ConfigService
import csw.services.config.server.ActorRuntime

/**
 * Routes supported by config server
 * @param configService              instance of config service to which the routes will delegate operations
 * @param actorRuntime               actor runtime
 * @param configHandlers     exception handler which maps server side exceptions to Http Status codes
 */
class ConfigServiceRoute(
    configService: ConfigService,
    actorRuntime: ActorRuntime,
    configHandlers: ConfigHandlers
) extends HttpSupport {

  import actorRuntime._

  def route: Route = routeLogger {
    handleExceptions(configHandlers.jsonExceptionHandler) {
      handleRejections(configHandlers.jsonRejectionHandler) {
        prefix("config") { filePath ⇒
          (get & rejectEmptyResponse) {
            (dateParam & idParam) {
              case (Some(date), _) ⇒
                complete(configService.getByTime(filePath, date))
              case (_, Some(id)) ⇒ complete(configService.getById(filePath, id))
              case (_, _)        ⇒ complete(configService.getLatest(filePath))
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
              complete(
                StatusCodes.Created -> configService
                  .create(filePath, configData, annex, comment)
              )
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
        (prefix("active-config") & get & rejectEmptyResponse) { filePath ⇒
          dateParam {
            case Some(date) ⇒
              complete(configService.getActiveByTime(filePath, date))
            case _ ⇒ complete(configService.getActive(filePath))
          }
        } ~
        prefix("active-version") { filePath ⇒
          put {
            (idParam & commentParam) {
              case (Some(configId), comment) ⇒
                complete(
                  configService
                    .setActiveVersion(filePath, configId, comment)
                    .map(_ ⇒ Done)
                )
              case (_, comment) ⇒
                complete(
                  configService
                    .resetActiveVersion(filePath, comment)
                    .map(_ ⇒ Done)
                )
            }
          } ~
          (get & rejectEmptyResponse) {
            complete(configService.getActiveVersion(filePath))
          }
        } ~
        (prefix("history") & get) { filePath ⇒
          (maxResultsParam & fromParam & toParam) { (maxCount, from, to) ⇒
            complete(configService.history(filePath, from, to, maxCount))
          }
        } ~
        (prefix("history-active") & get) { filePath ⇒
          (maxResultsParam & fromParam & toParam) { (maxCount, from, to) ⇒
            complete(configService.historyActive(filePath, from, to, maxCount))
          }
        } ~
        (path("list") & get) {
          (typeParam & patternParam) { (fileType, pattern) ⇒
            complete(configService.list(fileType, pattern))
          }
        } ~
        (path("metadata") & get) {
          complete(configService.getMetadata)
        }
      }
    }
  }
}
