package csw.aas.http

import akka.actor.typed
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import csw.aas.http.AuthorizationPolicy.RealmRolePolicy
import csw.location.api.scaladsl.LocationService

import scala.concurrent.{ExecutionContext, Future}

class TestServer(locationService: LocationService) {
  implicit lazy val actorSystem: typed.ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
  implicit lazy val ec: ExecutionContext                                  = actorSystem.executionContext
  private lazy val config                                                 = actorSystem.settings.config
  private lazy val securityDirectives                                     = SecurityDirectives(config, locationService)
  import securityDirectives._

  val routes: Route = get {
    complete("OK")
  } ~ sPost(RealmRolePolicy("admin")) {
    complete("OK")
  }

  def start(testServerPort: Int): Future[Http.ServerBinding] = {
    Http().bindAndHandle(routes, "0.0.0.0", testServerPort)
  }
}
