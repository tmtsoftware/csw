package csw.apps.clusterseed.admin.internal

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import csw.apps.clusterseed.admin.LogAdmin
import csw.apps.clusterseed.admin.http.{AdminHandlers, AdminHttpService, AdminRoutes}
import csw.apps.clusterseed.location.{LocationHttpService, LocationRoutes}
import csw.services.location.commons.ClusterSettings
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}

// $COVERAGE-OFF$
private[clusterseed] class AdminWiring {
  lazy val config: Config                     = ConfigFactory.load()
  lazy val settings                           = new Settings(config)
  lazy val clusterSettings                    = ClusterSettings()
  lazy val actorSystem: ActorSystem           = clusterSettings.system
  lazy val actorRuntime                       = new ActorRuntime(actorSystem)
  lazy val locationService: LocationService   = LocationServiceFactory.withSystem(actorSystem)
  lazy val logAdmin: LogAdmin                 = new LogAdmin(locationService, actorRuntime)
  lazy val adminHandlers                      = new AdminHandlers
  lazy val adminRoutes                        = new AdminRoutes(logAdmin, actorRuntime, adminHandlers)
  lazy val adminHttpService: AdminHttpService = new AdminHttpService(adminRoutes, actorRuntime, settings)
  lazy val locationRoutes                     = new LocationRoutes(locationService, actorRuntime)
  lazy val locationHttpService                = new LocationHttpService(locationRoutes, actorRuntime)
}

private[clusterseed] object AdminWiring {

  def make(_clusterSettings: ClusterSettings, maybeAdminPort: Option[Int]): AdminWiring =
    new AdminWiring {
      override lazy val clusterSettings: ClusterSettings = _clusterSettings

      override lazy val settings: Settings = new Settings(config) {
        override val adminPort: Int = maybeAdminPort.getOrElse(super.adminPort)
      }
    }

}
// $COVERAGE-ON$
