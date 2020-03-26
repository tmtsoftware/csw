package csw.location.server.internal

import akka.actor
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.server.Route
import com.typesafe.config.{Config, ConfigFactory}
import csw.aas.http.SecurityDirectives
import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.scaladsl.LocationService
import csw.location.server.commons.{ClusterAwareSettings, ClusterSettings}
import csw.location.server.http.{LocationHttpHandler, LocationHttpService, LocationWebsocketHandler}
import msocket.api.ContentType
import msocket.impl.RouteFactory
import msocket.impl.post.PostRouteFactory
import msocket.impl.ws.WebsocketRouteFactory

// $COVERAGE-OFF$
private[csw] class ServerWiring(enableAuth: Boolean) extends LocationServiceCodecs {
  lazy val config: Config                                           = ConfigFactory.load()
  lazy val settings                                                 = new Settings(config)
  lazy val clusterSettings: ClusterSettings                         = ClusterAwareSettings.onPort(settings.clusterPort)
  implicit lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = clusterSettings.system
  lazy val untypedActorSystem: actor.ActorSystem                    = clusterSettings.system.toClassic
  lazy val actorRuntime                                             = new ActorRuntime(actorSystem)
  import actorSystem.executionContext
  lazy val locationService: LocationService = LocationServiceFactory.withSystem(actorSystem)

  lazy val securityDirectives: SecurityDirectives = SecurityDirectives(locationService, !enableAuth)

  private lazy val postHandler                           = new LocationHttpHandler(locationService, securityDirectives)
  private def websocketHandler(contentType: ContentType) = new LocationWebsocketHandler(locationService, contentType)

  lazy val locationRoutes: Route = RouteFactory.combine(metricsEnabled = false)(
    new PostRouteFactory("post-endpoint", postHandler),
    new WebsocketRouteFactory("websocket-endpoint", websocketHandler)
  )
  lazy val locationHttpService = new LocationHttpService(locationRoutes, actorRuntime, settings)
}

private[csw] object ServerWiring {

  def make(maybeClusterPort: Option[Int], enableAuth: Boolean): ServerWiring =
    new ServerWiring(enableAuth) {
      override lazy val settings: Settings = new Settings(config) {
        override val clusterPort: Int = maybeClusterPort.getOrElse(super.clusterPort)
      }
    }

  def make(maybeClusterPort: Option[Int], mayBeHttpPort: Option[Int], enableAuth: Boolean): ServerWiring =
    new ServerWiring(enableAuth) {
      override lazy val settings: Settings = {
        new Settings(config) {
          override val clusterPort: Int = maybeClusterPort.getOrElse(super.clusterPort)
          override val httpPort: Int    = mayBeHttpPort.getOrElse(super.httpPort)
        }
      }
    }

  def make(_clusterSettings: ClusterSettings, enableAuth: Boolean): ServerWiring =
    new ServerWiring(enableAuth) {
      override lazy val clusterSettings: ClusterSettings = _clusterSettings
    }

  def make(_actorSystem: ActorSystem[SpawnProtocol.Command], enableAuth: Boolean): ServerWiring =
    new ServerWiring(enableAuth) {
      override lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = _actorSystem
    }
}
// $COVERAGE-ON$
