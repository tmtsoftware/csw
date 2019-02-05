package csw.auth

import akka.actor.ActorSystem
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import csw.aas.http.AuthorizationPolicy._
import csw.aas.http.{AuthorizationPolicy, SecurityDirectives}
import csw.auth.AsyncSupport.actorSystem
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.client.utils.LocationServerStatus
import csw.logging.client.scaladsl.LoggingSystemFactory

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
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

object Documentation extends HttpApp {

  import AsyncSupport._

  private val locationService = HttpLocationServiceFactory.makeLocalClient

  private val directives = SecurityDirectives(locationService)

  import directives._

  object Database {
    def doesUserOwnFile(userId: Option[String], fileId: Long): Future[Boolean] = ???

    def getFileContents(fileId: Long): String = ???
  }

  object ThirdPartyService {
    def deleteEntity(entityId: Long, username: Option[String]) = ???
    def deleteEntity(entityId: Long)                           = ???
  }

  // #access-token-handle-demo
  val routeExampleWithToken: Route = sDelete(EmptyPolicy) { token =>
    parameter("entityId".as[Long]) { entityId =>
      ThirdPartyService.deleteEntity(entityId, token.preferred_username)
      complete(s"user ${token.given_name} ${token.family_name} deleted entity $entityId")
    }
  }

  val routeExampleWithoutToken: Route = sDelete(EmptyPolicy) {
    parameter("entityId".as[Long]) { entityId =>
      ThirdPartyService.deleteEntity(entityId)
      complete(s"entity $entityId deleted")
    }
  }
  // #access-token-handle-demo

  // #custom-policy-async
  //GET http://[host]:[port]/files?fileId=[fileId]
  val routes: Route =
    path("files" / LongNumber) { fileId =>
      sGet(CustomPolicyAsync(token => Database.doesUserOwnFile(token.preferred_username, fileId))) {
        complete(Database.getFileContents(fileId))
      }
    }
  // #custom-policy-async

  // #empty-policy-usage
  val authenticationOnlyRoute: Route = // GET http://[host]:[post]/api
    path("api") {
      sGet(EmptyPolicy) {
        complete("OK")
      }
    }
  // #empty-policy-usage

  // #secure-route-example
  val secureRoute: Route = sPost(RealmRolePolicy("example-admin-role")) {
    complete("SUCCESS")
  }
  // #secure-route-example

  object PolicyExpressions {
    // #policy-expressions
    val routes: Route =
    sGet(RealmRolePolicy("admin") | CustomPolicy(_.email.contains("super-admin@tmt.org"))) {
      complete("OK")
    } ~
    sPost(ClientRolePolicy("finance_user") & PermissionPolicy("edit")) {
      complete("OK")
    }
    // #policy-expressions
  }

  val list = List(
    // #realm-role-policy
    RealmRolePolicy("admin"),
    // #realm-role-policy
    // #client-role-policy
    ClientRolePolicy("accounts-admin"),
    // #client-role-policy
    // #custom-policy
    CustomPolicy(token ⇒ token.given_name.contains("test-user")),
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
