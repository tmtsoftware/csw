package csw.aas.http
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import csw.aas.http.AuthorizationPolicy.RealmRolePolicy
import csw.location.api.scaladsl.LocationService
import csw.logging.core.scaladsl.LoggingSystemFactory

import scala.concurrent.{ExecutionContext, Future}

class TestServer(locationService: LocationService)(implicit ec: ExecutionContext) {
  val securityDirectives = SecurityDirectives(locationService)
  import securityDirectives._

  val routes: Route = get {
    complete("OK")
  } ~ sPost(RealmRolePolicy("admin")) {
    complete("OK")
  }

  def start(testServerPort: Int): Future[Http.ServerBinding] = {

    implicit val system: ActorSystem = ActorSystem()

    LoggingSystemFactory.start("test-server", "", "", system)
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    Http().bindAndHandle(routes, "localhost", testServerPort)
  }
}
