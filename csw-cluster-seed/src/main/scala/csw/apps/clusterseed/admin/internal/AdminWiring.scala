package csw.apps.clusterseed.admin.internal

import com.typesafe.config.{Config, ConfigFactory}
import csw.apps.clusterseed.admin.LogAdmin
import csw.apps.clusterseed.admin.http.{AdminExceptionHandler, AdminHttpService, AdminRoutes}
import csw.services.location.commons.ClusterSettings
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}

/**
 * Admin app wiring
 */
class AdminWiring {

  lazy val config: Config                     = ConfigFactory.load()
  lazy val settings                           = new Settings(config)
  lazy val clusterSettings                    = ClusterSettings()
  lazy val actorSystem                        = clusterSettings.system
  lazy val actorRuntime                       = new ActorRuntime(actorSystem)
  lazy val locationService: LocationService   = LocationServiceFactory.withSystem(actorSystem)
  lazy val logAdmin: LogAdmin                 = new LogAdmin(locationService, actorRuntime)
  lazy val adminExceptionHandler              = new AdminExceptionHandler
  lazy val adminRoutes                        = new AdminRoutes(adminExceptionHandler, logAdmin, actorRuntime)
  lazy val adminHttpService: AdminHttpService = new AdminHttpService(adminRoutes, actorRuntime, settings)
}

object AdminWiring {

  def make(_clusterSettings: ClusterSettings, maybeAdminPort: Option[Int]): AdminWiring =
    new AdminWiring {
      override lazy val clusterSettings: ClusterSettings = _clusterSettings

      override lazy val settings: Settings = new Settings(config) {
        override val `admin-port`: Int = maybeAdminPort.getOrElse(super.`admin-port`)
      }
    }

}
