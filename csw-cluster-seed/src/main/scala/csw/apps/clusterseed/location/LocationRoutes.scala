package csw.apps.clusterseed.location

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import csw.apps.clusterseed.admin.internal.ActorRuntime
import csw.messages.location.Connection
import csw.services.location.internal.LocationJsonSupport
import csw.services.location.models.Registration
import csw.services.location.scaladsl.LocationService
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._

class LocationRoutes(locationService: LocationService, actorRuntime: ActorRuntime)
    extends FailFastCirceSupport
    with LocationJsonSupport {

  import actorRuntime._

  val routes: Route = pathPrefix("location") {
    get {
      path("list") {
        complete(locationService.list)
      }
    } ~
    post {
      path("register") {
        entity(as[Registration]) { registration =>
          complete(locationService.register(registration).map(_.location))
        }
      } ~
      path("unregister") {
        entity(as[Connection]) { connection =>
          complete(locationService.unregister(connection))
        }
      } ~
      path("unregisterAll") {
        complete(locationService.unregisterAll())
      }
    }
  }
}
