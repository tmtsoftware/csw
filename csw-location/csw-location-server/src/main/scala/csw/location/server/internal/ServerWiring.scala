package csw.location.server.internal

import akka.actor
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.server.Route
import com.typesafe.config.{Config, ConfigFactory}
import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.scaladsl.LocationService
import csw.location.server.commons.{ClusterAwareSettings, ClusterSettings}
import csw.location.server.http.{LocationHttpHandler, LocationHttpService, LocationWebsocketHandler}
import msocket.impl.post.PostRouteFactory
import msocket.impl.ws.WebsocketRouteFactory
import msocket.impl.{Encoding, RouteFactory}

// $COVERAGE-OFF$
private[csw] class ServerWiring extends LocationServiceCodecs {
  lazy val config: Config                                           = ConfigFactory.load()
  lazy val settings                                                 = new Settings(config)
  lazy val clusterSettings: ClusterSettings                         = ClusterAwareSettings.onPort(settings.clusterPort)
  implicit lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = clusterSettings.system
  lazy val untypedActorSystem: actor.ActorSystem                    = clusterSettings.system.toClassic
  lazy val actorRuntime                                             = new ActorRuntime(actorSystem)
  import actorSystem.executionContext
  lazy val locationService: LocationService                  = LocationServiceFactory.withSystem(actorSystem)
  private val postHandler                                    = new LocationHttpHandler(locationService)
  private def websocketHandlerFactory(encoding: Encoding[_]) = new LocationWebsocketHandler(locationService, encoding)

  lazy val locationRoutes: Route = RouteFactory.combine(
    new PostRouteFactory("post-endpoint", postHandler),
    new WebsocketRouteFactory("websocket-endpoint", websocketHandlerFactory)
  )
  lazy val locationHttpService = new LocationHttpService(locationRoutes, actorRuntime, settings)
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

  def make(_actorSystem: ActorSystem[SpawnProtocol.Command]): ServerWiring =
    new ServerWiring {
      override lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = _actorSystem
    }
}
// $COVERAGE-ON$
