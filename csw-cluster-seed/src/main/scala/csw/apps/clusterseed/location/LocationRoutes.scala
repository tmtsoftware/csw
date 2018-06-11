package csw.apps.clusterseed.location

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import csw.services.location.models.Registration
import csw.services.location.scaladsl.LocationService
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._

class LocationRoutes(locationService: LocationService)(implicit actorSystem: ActorSystem)
    extends FailFastCirceSupport
    with LocationJsonSupport {

  import actorSystem.dispatcher

  val routes: Route = pathPrefix("location") {
    get {
      path("asdasd") {
        complete("ok")
      }
    } ~
    post {
      path("register") {
        entity(as[Registration]) { registration =>
          complete(locationService.register(registration).map(_.location))
        }
      }
    }
  }
}
