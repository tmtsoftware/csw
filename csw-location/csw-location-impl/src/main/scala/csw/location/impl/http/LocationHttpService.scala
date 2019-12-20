package csw.location.impl.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import csw.location.impl.internal.{ActorRuntime, Settings}

import scala.concurrent.Future

class LocationHttpService(locationRoutes: Route, actorRuntime: ActorRuntime, settings: Settings) {

  implicit val classicSystem: ActorSystem = actorRuntime.classicSystem

  // todo: parameterize interface
  def start(): Future[Http.ServerBinding] = {
    Http().bindAndHandle(
      handler = cors()(locationRoutes),
      interface = "0.0.0.0",
      port = settings.httpPort
    )
  }
}
