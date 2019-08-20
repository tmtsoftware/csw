package csw.location.server.internal

import akka.actor
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import com.typesafe.config.{Config, ConfigFactory}
import csw.location.api.scaladsl.LocationService
import csw.location.server.commons.{ClusterAwareSettings, ClusterSettings}
import csw.location.server.http.{LocationExceptionHandler, LocationHttpService, LocationRoutes}

// $COVERAGE-OFF$
private[csw] class ServerWiring {
  lazy val config: Config                          = ConfigFactory.load()
  lazy val settings                                = new Settings(config)
  lazy val clusterSettings: ClusterSettings        = ClusterAwareSettings.onPort(settings.clusterPort)
  lazy val actorSystem: ActorSystem[SpawnProtocol] = clusterSettings.system
  lazy val untypedActorSystem: actor.ActorSystem   = clusterSettings.system.toUntyped
  lazy val actorRuntime                            = new ActorRuntime(actorSystem)
  lazy val locationService: LocationService        = LocationServiceFactory.withSystem(actorSystem)
  lazy val locationExceptionHandler                = new LocationExceptionHandler
  lazy val locationRoutes                          = new LocationRoutes(locationService, locationExceptionHandler, actorRuntime)
  lazy val locationHttpService                     = new LocationHttpService(locationRoutes, actorRuntime, settings)
}

private[csw] object ServerWiring {

  def make(maybeClusterPort: Option[Int]): ServerWiring =
    new ServerWiring {
      override lazy val settings: Settings = new Settings(config) {
        override val clusterPort: Int = maybeClusterPort.getOrElse(super.clusterPort)
      }
    }

  def make(maybeClusterPort: Option[Int], mayBeHttpPort: Option[Int]): ServerWiring =
    new ServerWiring {
      override lazy val settings: Settings = {
        new Settings(config) {
          override val clusterPort: Int = maybeClusterPort.getOrElse(super.clusterPort)
          override val httpPort: Int    = mayBeHttpPort.getOrElse(super.httpPort)
        }
      }
    }

  def make(_clusterSettings: ClusterSettings): ServerWiring =
    new ServerWiring {
      override lazy val clusterSettings: ClusterSettings = _clusterSettings
    }

  def make(_actorSystem: ActorSystem[SpawnProtocol]): ServerWiring =
    new ServerWiring {
      override lazy val actorSystem: ActorSystem[SpawnProtocol] = _actorSystem
    }
}
// $COVERAGE-ON$
