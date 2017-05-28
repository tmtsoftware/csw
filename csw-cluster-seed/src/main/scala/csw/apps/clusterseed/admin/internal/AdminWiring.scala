package csw.apps.clusterseed.admin.internal

import akka.actor.ActorSystem
import csw.apps.clusterseed.admin.LogAdmin
import csw.apps.clusterseed.admin.http.{AdminExceptionHandler, AdminHttpService, AdminRoutes}
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}

/**
 * Admin app wiring
 */
class AdminWiring(actorSystem: ActorSystem) {
  lazy val actorRuntime                       = new ActorRuntime(actorSystem)
  lazy val locationService: LocationService   = LocationServiceFactory.withSystem(actorSystem)
  lazy val logAdmin: LogAdmin                 = new LogAdmin(locationService, actorRuntime)
  lazy val adminExceptionHandler              = new AdminExceptionHandler
  lazy val adminRoutes                        = new AdminRoutes(adminExceptionHandler, logAdmin, actorRuntime)
  lazy val adminHttpService: AdminHttpService = new AdminHttpService(adminRoutes, actorRuntime)
}
