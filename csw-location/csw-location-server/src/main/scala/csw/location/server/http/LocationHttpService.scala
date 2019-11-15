package csw.location.server.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import csw.location.server.internal.{ActorRuntime, Settings}

import scala.concurrent.Future

class LocationHttpService(locationRoutes: LocationRoutes, actorRuntime: ActorRuntime, settings: Settings) {
  implicit val classicSystem: ActorSystem = actorRuntime.classicSystem
  def start(): Future[Http.ServerBinding] = {
    Http().bindAndHandle(
      handler = locationRoutes.routes,
      interface = "0.0.0.0",
      port = settings.httpPort
    )
  }
}
