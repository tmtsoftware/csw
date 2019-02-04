package csw.auth

import akka.actor.ActorSystem
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import csw.aas.http.AuthorizationPolicy._
import csw.aas.http.SecurityDirectives
import csw.auth.AsyncSupport.actorSystem
import csw.auth.ExampleServer.{complete, get, pathPrefix, startServer}
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.client.utils.LocationServerStatus
import csw.logging.client.scaladsl.LoggingSystemFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.language.implicitConversions

object ExampleServer extends HttpApp with App {

  import AsyncSupport._

  // #security-directive-usage
  val locationService = HttpLocationServiceFactory.makeLocalClient

  val directives = SecurityDirectives(locationService)

  import directives._
  // #security-directive-usage

  val HOST = "0.0.0.0"
  val PORT = 9003

  // #example-routes
  override protected def routes: Route = cors() {
    pathPrefix("person") {
      get {
        complete("SUCCESS")
      } ~
      sPost(RealmRolePolicy("example-admin-role")) {
        complete("SUCCESS")
      } ~
      sPut(ClientRolePolicy("person-role")) {
        complete("SUCCESS")
      } ~
      sPatch(ClientRolePolicy("some-role")) {
        complete("SUCCESS")
      } ~
      sHead(CustomPolicy(at ⇒ at.given_name.contains("test-user"))) {
        complete("SUCCESS")
      } ~
      sDelete(PermissionPolicy("delete", "person")) {
        complete("SUCCESS")
      } ~
      sGet(EmptyPolicy) {
        complete("SUCCESS")
      }
    }
  }
  // #example-routes
  startServer(HOST, PORT)
}

object LoggingSupport {
  //start logging
  LoggingSystemFactory.start("example-server", "", "", actorSystem)
}

object AsyncSupport {
  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val ec: ExecutionContext     = ExecutionContext.global
  implicit val mat: ActorMaterializer   = ActorMaterializer()
}

object LocationServiceSupport {
  //ensure location service is up
  LocationServerStatus.requireUpLocally(5.seconds)
}

object Documentation {

  import AsyncSupport._

  private val locationService = HttpLocationServiceFactory.makeLocalClient

  private val directives = SecurityDirectives(locationService)

  import directives._

  // #secure-route-example
  val secureRoute: Route = sPost(RealmRolePolicy("example-admin-role")) {
    complete("SUCCESS")
  }
  // #secure-route-example

  val list = List(
    // #realm-role-policy
    RealmRolePolicy("admin"),
    // #realm-role-policy
    // #client-role-policy
    ClientRolePolicy("accounts-admin"),
    // #client-role-policy
    // #custom-policy
    CustomPolicy(at ⇒ at.given_name.contains("test-user")),
    // #custom-policy
    // #permission-policy
    PermissionPolicy("delete", "account"),
    // #permission-policy
    // #empty-policy
    EmptyPolicy
    // #empty-policy
  )
}

// #sample-http-app
object SampleHttpApp extends HttpApp with App {

  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val ec: ExecutionContext     = ExecutionContext.global
  implicit val mat: ActorMaterializer   = ActorMaterializer()

  val locationService = HttpLocationServiceFactory.makeLocalClient
  val directives      = SecurityDirectives(locationService)
  import directives._

  override protected def routes: Route = pathPrefix("api") {
    get {
      complete("SUCCESS")
    } ~
    sPost(RealmRolePolicy("admin")) {
      complete("SUCCESS")
    }
  }

  startServer("0.0.0.0", 9003)
}
// #sample-http-app
