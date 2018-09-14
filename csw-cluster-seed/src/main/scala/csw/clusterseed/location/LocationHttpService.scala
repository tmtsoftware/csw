package csw.clusterseed.location

import akka.http.scaladsl.Http
import csw.clusterseed.internal.{ActorRuntime, Settings}

import scala.concurrent.Future

class LocationHttpService(locationRoutes: LocationRoutes, actorRuntime: ActorRuntime, settings: Settings) {

  import actorRuntime._

  def start(): Future[Http.ServerBinding] = {
    Http().bindAndHandle(
      handler = locationRoutes.routes,
      interface = "0.0.0.0",
      port = settings.httpLocationPort
    )
  }
}
