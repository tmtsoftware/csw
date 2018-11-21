package csw.config.server.http

import akka.Done
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directive0, Route}
import csw.auth.adapters.akka.http.SecurityDirectives
import csw.config.api.scaladsl.ConfigService
import csw.config.server.ActorRuntime

/**
 * Routes supported by config server
 *
 * @param configService instance of config service to which the routes will delegate operations
 * @param actorRuntime actorRuntime provides runtime accessories related to ActorSystem like Materializer, ExecutionContext etc.
 * @param configHandlers exception handler which maps server side exceptions to Http Status codes
 */
class ConfigServiceRoute(
    configService: ConfigService,
    actorRuntime: ActorRuntime,
    configHandlers: ConfigHandlers,
    securityDirectives: SecurityDirectives
) extends HttpSupport {

  import actorRuntime._

  private val adminProtected: Directive0 = securityDirectives.resourceRole("admin")

  def route: Route = routeLogger {
    handleExceptions(configHandlers.jsonExceptionHandler) {
      handleRejections(configHandlers.jsonRejectionHandler) {

        prefix("config") { filePath ⇒
          (get & rejectEmptyResponse) { // fetch the file - http://{{hostname}}:{{port}}/config/{{path}}
            (dateParam & idParam) {
              case (Some(date), _) ⇒ complete(configService.getByTime(filePath, date))
              case (_, Some(id))   ⇒ complete(configService.getById(filePath, id))
              case (_, _)          ⇒ complete(configService.getLatest(filePath))
            }
          } ~
          head { // check if file exists - http://{{hostname}}:{{port}}/config/{{path}}
            idParam { id ⇒
              complete {
                configService.exists(filePath, id).map { found ⇒
                  if (found) StatusCodes.OK else StatusCodes.NotFound
                }
              }
            }
          } ~
          (post & adminProtected) { // create file - http://{{hostname}}:{{port}}/config/{{path}}?comment="Sample commit message"
            (configDataEntity & annexParam & commentParam) { (configData, annex, comment) ⇒
              complete(
                StatusCodes.Created -> configService
                  .create(filePath, configData, annex, comment)
              )
            }
          } ~
          (put & adminProtected) { // update file - http://{{hostname}}:{{port}}/config/{{path}}?comment="Sample update commit message"
            (configDataEntity & commentParam) { (configData, comment) ⇒
              complete(configService.update(filePath, configData, comment))
            }
          } ~
          (delete & adminProtected) { // delete file - http://{{hostname}}:{{port}}/config/{{path}}?comment="deleting config file"
            commentParam { comment ⇒
              complete(configService.delete(filePath, comment).map(_ ⇒ Done))
            }
          }

        } ~
        (prefix("active-config") & get & rejectEmptyResponse) { filePath ⇒
          dateParam { // fetch the currently active file - http://{{hostname}}:{{port}}/active-config/{{path}}
            case Some(date) ⇒
              complete(configService.getActiveByTime(filePath, date))
            case _ ⇒ complete(configService.getActive(filePath))
          }
        } ~
        prefix("active-version") { filePath ⇒
          (put & adminProtected) { // set the active version - http://{{hostname}}:{{port}}/active-version/{{path}}?id=3&comment="Setting activer version"
            (idParam & commentParam) {
              case (Some(configId), comment) ⇒ complete(configService.setActiveVersion(filePath, configId, comment).map(_ ⇒ Done))
              case (_, comment)              ⇒ complete(configService.resetActiveVersion(filePath, comment).map(_ ⇒ Done))
            }
          } ~
          (get & rejectEmptyResponse) { // fetch the active version - http://{{hostname}}:{{port}}/active-version/{{path}}
            complete(configService.getActiveVersion(filePath))
          }
        } ~
        (prefix("history") & get) { filePath ⇒
          (maxResultsParam & fromParam & toParam) { (maxCount, from, to) ⇒ // fetch the history of file - http://{{hostname}}:{{port}}/history/{{path}}
            complete(configService.history(filePath, from, to, maxCount))
          }
        } ~
        (prefix("history-active") & get) { filePath ⇒ // fetch the history of active version - http://{{hostname}}:{{port}}/history-active/{{path}}
          (maxResultsParam & fromParam & toParam) { (maxCount, from, to) ⇒
            complete(configService.historyActive(filePath, from, to, maxCount))
          }
        } ~
        (path("list") & get) { // list all files based on file type i.e.'Normal' or 'Annex' and/or pattern if provided - http://{{hostname}}:{{port}}/list
          (typeParam & patternParam) { (fileType, pattern) ⇒
            complete(configService.list(fileType, pattern))
          }
        } ~
        (path("metadata") & get) { // fetch the metadata of config server - http://{{hostname}}:{{port}}/metadata
          complete(configService.getMetadata)
        }
      }
    }
  }
}
