package csw.aas.http

import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.actor.{typed, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.typed.scaladsl.ActorMaterializer
import csw.aas.http.AuthorizationPolicy.RealmRolePolicy
import csw.location.api.scaladsl.LocationService
import csw.logging.client.scaladsl.LoggingSystemFactory

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

    implicit val system: ActorSystem                    = ActorSystem()
    implicit val typedActorSystem: typed.ActorSystem[_] = ActorSystem().toTyped

    LoggingSystemFactory.start("test-server", "", "", typedActorSystem)
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    Http().bindAndHandle(routes, "0.0.0.0", testServerPort)
  }
}
