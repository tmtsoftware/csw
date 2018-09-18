package csw.clusterseed.internal

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import csw.clusterseed.admin.LogAdmin
import csw.clusterseed.admin.http.{AdminExceptionHandlers, AdminHttpService, AdminRoutes}
import csw.clusterseed.location.{LocationExceptionHandler, LocationHttpService, LocationRoutes}
import csw.location.api.scaladsl.LocationService
import csw.location.api.commons.{ClusterAwareSettings, ClusterSettings}
import csw.location.scaladsl.LocationServiceFactory

// $COVERAGE-OFF$
private[clusterseed] class AdminWiring {
  lazy val config: Config                     = ConfigFactory.load()
  lazy val settings                           = new Settings(config)
  lazy val clusterSettings: ClusterSettings   = ClusterAwareSettings.onPort(settings.clusterPort)
  lazy val actorSystem: ActorSystem           = clusterSettings.system
  lazy val actorRuntime                       = new ActorRuntime(actorSystem)
  lazy val locationService: LocationService   = LocationServiceFactory.withSystem(actorSystem)
  lazy val logAdmin: LogAdmin                 = new LogAdmin(locationService, actorRuntime)
  lazy val adminHandlers                      = new AdminExceptionHandlers
  lazy val adminRoutes                        = new AdminRoutes(logAdmin, actorRuntime, adminHandlers)
  lazy val adminHttpService: AdminHttpService = new AdminHttpService(adminRoutes, actorRuntime, settings)
  lazy val locationExceptionHandler           = new LocationExceptionHandler
  lazy val locationRoutes                     = new LocationRoutes(locationService, locationExceptionHandler, actorRuntime)
  lazy val locationHttpService                = new LocationHttpService(locationRoutes, actorRuntime, settings)
}

private[clusterseed] object AdminWiring {

  def make(maybeClusterPort: Option[Int], maybeAdminPort: Option[Int]): AdminWiring =
    new AdminWiring {
      override lazy val settings: Settings = new Settings(config) {
        override val clusterPort: Int = maybeClusterPort.getOrElse(super.clusterPort)
        override val adminPort: Int   = maybeAdminPort.getOrElse(super.adminPort)
      }
    }
}
// $COVERAGE-ON$
