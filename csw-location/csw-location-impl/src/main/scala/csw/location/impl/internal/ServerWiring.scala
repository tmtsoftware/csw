package csw.location.impl.internal

import akka.actor
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.server.Route
import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.scaladsl.LocationService
import csw.location.impl.commons.{ClusterAwareSettings, ClusterSettings}
import csw.location.impl.http.{LocationHttpHandler, LocationHttpService, LocationWebsocketHandler}
import msocket.api.Encoding
import msocket.impl.RouteFactory
import msocket.impl.post.PostRouteFactory
import msocket.impl.ws.WebsocketRouteFactory

// $COVERAGE-OFF$
class ServerWiring(settings: Settings) extends LocationServiceCodecs {
  lazy val clusterSettings: ClusterSettings                         = ClusterAwareSettings.onPort(settings.clusterPort)
  implicit lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = clusterSettings.system
  lazy val untypedActorSystem: actor.ActorSystem                    = clusterSettings.system.toClassic
  lazy val actorRuntime                                             = new ActorRuntime(actorSystem)
  import actorSystem.executionContext
  lazy val locationService: LocationService                  = LocationServiceFactory.withSystem(actorSystem)
  private lazy val postHandler                               = new LocationHttpHandler(locationService)
  private def websocketHandlerFactory(encoding: Encoding[_]) = new LocationWebsocketHandler(locationService, encoding)

  lazy val locationRoutes: Route = RouteFactory.combine(
    new PostRouteFactory("post-endpoint", postHandler),
    new WebsocketRouteFactory("websocket-endpoint", websocketHandlerFactory)
  )
  lazy val locationHttpService = new LocationHttpService(locationRoutes, actorRuntime, settings)
}

object ServerWiring {

  def make(maybeClusterPort: Option[Int], configKey: String): ServerWiring =
    new ServerWiring(Settings(configKey).withClusterPort(maybeClusterPort))

  def make(maybeClusterPort: Option[Int], mayBeHttpPort: Option[Int], configKey: String): ServerWiring =
    new ServerWiring(
      Settings(configKey)
        .withHttpPort(mayBeHttpPort)
        .withClusterPort(maybeClusterPort)
    )

  def make(_clusterSettings: ClusterSettings, configKey: String): ServerWiring = {
    val settings = Settings(configKey)

    new ServerWiring(settings) {
      override lazy val clusterSettings: ClusterSettings = _clusterSettings
    }
  }
  def make(_actorSystem: ActorSystem[SpawnProtocol.Command], configKey: String): ServerWiring = {
    val settings = Settings(configKey)

    new ServerWiring(settings) {
      override lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = _actorSystem
    }
  }
}
// $COVERAGE-ON$
