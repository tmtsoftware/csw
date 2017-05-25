package csw.services.config.server

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import csw.services.admin.LogAdmin
import csw.services.admin.http.{AdminExceptionHandler, AdminHttpService, AdminRoutes}
import csw.services.location.commons.{ClusterSettings, CswCluster}
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}

/**
 * Server configuration
 */
class AdminWiring {

  lazy val clusterSettings                  = ClusterSettings()
  implicit val actorSystem                  = ActorSystem(clusterSettings.clusterName, clusterSettings.config)
  implicit val mat: Materializer            = ActorMaterializer()
  lazy val cswCluster: CswCluster           = CswCluster.withSystem(actorSystem)
  lazy val locationService: LocationService = LocationServiceFactory.withCluster(cswCluster)

  lazy val logAdmin: LogAdmin    = new LogAdmin(locationService)
  lazy val adminExceptionHandler = new AdminExceptionHandler
  lazy val adminRoutes           = new AdminRoutes(adminExceptionHandler, logAdmin)

  lazy val adminHttpService: AdminHttpService =
    new AdminHttpService(locationService, adminRoutes)
}

object AdminWiring {

  def make(_clusterSettings: ClusterSettings): AdminWiring = new AdminWiring {
    override lazy val clusterSettings: ClusterSettings = _clusterSettings
  }

  def make(_clusterSettings: ClusterSettings, maybePort: Option[Int]): AdminWiring = new AdminWiring {
    override lazy val clusterSettings: ClusterSettings = _clusterSettings
  }

}
