package csw.location.server.http

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.handleRejections
import akka.http.scaladsl.server.{RejectionHandler, Route}
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import csw.location.server.internal.{ActorRuntime, Settings}

import scala.concurrent.Future

class LocationHttpService(locationRoutes: Route, actorRuntime: ActorRuntime, settings: Settings) {

  implicit val actorSystem = actorRuntime.actorSystem

  private def applicationRoute: Route = {
    val rejectionHandler = corsRejectionHandler.withFallback(RejectionHandler.default)
    cors() {
      handleRejections(rejectionHandler) {
        locationRoutes
      }
    }
  }

  def start(httpBindHost: String = "127.0.0.1"): Future[Http.ServerBinding] = {

    Http().bindAndHandle(
      handler = applicationRoute,
      interface = httpBindHost,
      port = settings.httpPort
    )
  }
}
