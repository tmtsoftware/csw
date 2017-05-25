package csw.services.config.server

import akka.actor.ActorSystem
import csw.services.admin.http.{AdminExceptionHandler, AdminHttpService, AdminRoutes}
import csw.services.admin.{ActorRuntime, LogAdmin}
import csw.services.location.commons.{ClusterSettings, CswCluster}
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}

/**
 * Server configuration
 */
class AdminWiring {

  lazy val clusterSettings                  = ClusterSettings()
  lazy val actorSystem                      = ActorSystem(clusterSettings.clusterName, clusterSettings.config)
  lazy val actorRuntime                     = new ActorRuntime(actorSystem)
  lazy val cswCluster: CswCluster           = CswCluster.withSystem(actorSystem)
  lazy val locationService: LocationService = LocationServiceFactory.withCluster(cswCluster)

  lazy val logAdmin: LogAdmin    = new LogAdmin(locationService, actorRuntime)
  lazy val adminExceptionHandler = new AdminExceptionHandler
  lazy val adminRoutes           = new AdminRoutes(adminExceptionHandler, logAdmin, actorRuntime)

  lazy val adminHttpService: AdminHttpService =
    new AdminHttpService(locationService, adminRoutes, actorRuntime)
}

object AdminWiring {

  def make(_clusterSettings: ClusterSettings): AdminWiring = new AdminWiring {
    override lazy val clusterSettings: ClusterSettings = _clusterSettings
  }

  def make(_clusterSettings: ClusterSettings, maybePort: Option[Int]): AdminWiring = new AdminWiring {
    override lazy val clusterSettings: ClusterSettings = _clusterSettings
  }

}
