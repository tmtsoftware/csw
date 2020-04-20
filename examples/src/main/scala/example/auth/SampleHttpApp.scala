package example.auth

import akka.actor.typed
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.server.{HttpApp, Route}
import csw.aas.http.AuthorizationPolicy.RealmRolePolicy
import csw.aas.http.SecurityDirectives
import csw.location.client.scaladsl.HttpLocationServiceFactory

import scala.concurrent.ExecutionContext

// #sample-http-app
object SampleHttpApp extends HttpApp with App {

  implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = typed.ActorSystem(SpawnProtocol(), "sample-http-app")
  implicit val ec: ExecutionContext                            = actorSystem.executionContext
  private val config                                           = actorSystem.settings.config

  val locationService = HttpLocationServiceFactory.makeLocalClient
  val directives      = SecurityDirectives(config, locationService)
  import directives._

  override protected def routes: Route = pathPrefix("api") {
    get {
      complete("SUCCESS")
    } ~
    sPost(RealmRolePolicy("admin")) {
      complete("SUCCESS")
    }
  }

  private val host = "0.0.0.0"
  private val port = 9003

  startServer(host, port)
}
// #sample-http-app
