package csw.admin.server.log.http

import akka.Done
import akka.http.scaladsl.server.Route
import csw.admin.server.log.LogAdmin
import csw.admin.server.wiring.ActorRuntime
import csw.logging.models.codecs.LoggingCodecs

/**
 * Routes supported by Admin server
 *
 * @param logAdmin instance of log admin to which the routes will delegate operations
 * @param actorRuntime actorRuntime provides runtime accessories related to ActorSystem like ExecutionContext etc.
 * @param adminExceptionHandlers exception handler which maps server side exceptions to Http Status codes
 */
class AdminRoutes(logAdmin: LogAdmin, actorRuntime: ActorRuntime, adminExceptionHandlers: AdminExceptionHandlers)
    extends HttpParameter
    with LoggingCodecs
    with HttpCodecs {

  import actorRuntime._
  val route: Route = routeLogger {
    adminExceptionHandlers.route {
      path("admin" / "logging" / Segment / "level") { connectionName =>
        // connectionName should be prefixHandle-componentType-connectionType.   see ConnectionInfo.scala
        get {
          complete(logAdmin.getLogMetadata(connectionName))
        } ~
        post {
          logLevelParam { logLevel =>
            complete(logAdmin.setLogLevel(connectionName, logLevel).map(_ => Done))
          }
        }
      }
    }
  }
}
