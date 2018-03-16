package csw.apps.clusterseed.admin.http

import akka.Done
import akka.http.scaladsl.server.Route
import csw.apps.clusterseed.admin.LogAdmin
import csw.apps.clusterseed.admin.internal.ActorRuntime

/**
 * Routes supported by Admin server
 *
 * @param logAdmin instance of log admin to which the routes will delegate operations
 * @param actorRuntime actorRuntime provides runtime accessories related to ActorSystem like Materializer, ExecutionContext etc.
 * @param adminHandlers exception handler which maps server side exceptions to Http Status codes
 */
class AdminRoutes(logAdmin: LogAdmin, actorRuntime: ActorRuntime, adminHandlers: AdminHandlers) extends HttpSupport {

  import actorRuntime._
  val route: Route = routeLogger {
    handleExceptions(adminHandlers.jsonExceptionHandler) {
      handleRejections(adminHandlers.jsonRejectionHandler) {
        path("admin" / "logging" / Segment / "level") { componentName ⇒
          get {
            complete(logAdmin.getLogMetadata(componentName))
          } ~
          post {
            logLevelParam { (logLevel) ⇒
              complete(logAdmin.setLogLevel(componentName, logLevel).map(_ ⇒ Done))
            }
          }
        }
      }
    }
  }

}
