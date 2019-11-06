package csw.location.server.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import csw.location.server.internal.{ActorRuntime, Settings}

import scala.concurrent.Future

class LocationHttpService(locationRoutes: Route, actorRuntime: ActorRuntime, settings: Settings) {

  implicit val classicSystem: ActorSystem = actorRuntime.classicSystem

  def start(): Future[Http.ServerBinding] = {
    Http().bindAndHandle(
      handler = locationRoutes,
      interface = "0.0.0.0",
      port = settings.httpPort
    )
  }
}
