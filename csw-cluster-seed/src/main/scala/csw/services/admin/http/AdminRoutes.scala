package csw.services.admin.http

import akka.Done
import akka.http.scaladsl.server.Route
import csw.services.admin.{ActorRuntime, LogAdmin}

class AdminRoutes(adminExceptionHandler: AdminExceptionHandler, logAdmin: LogAdmin, actorRuntime: ActorRuntime)
    extends HttpSupport {

  import actorRuntime._
  val route: Route = handleExceptions(adminExceptionHandler.exceptionHandler) {
    path("admin" / "logging" / Segment / "level") { componentName ⇒
      get {
        complete(logAdmin.getLogLevel(componentName))
      } ~
      post {
        logLevelParam { (logLevel) ⇒
          complete(logAdmin.setLogLevel(componentName, logLevel).map(_ ⇒ Done))
        }
      }
    }
  }
}
