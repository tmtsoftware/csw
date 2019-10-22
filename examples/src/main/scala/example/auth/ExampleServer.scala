package example.auth

import akka.actor.typed
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.server._
import akka.stream.Materializer
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import csw.aas.http.AuthorizationPolicy._
import csw.aas.http.SecurityDirectives
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.client.utils.LocationServerStatus
import csw.logging.client.scaladsl.LoggingSystemFactory
import example.auth.AsyncSupport.actorSystem

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

object ExampleServer extends HttpApp with App {

  import AsyncSupport._

  val locationService = HttpLocationServiceFactory.makeLocalClient

  val directives = SecurityDirectives(locationService)

  import directives._

  val HOST = "0.0.0.0"
  val PORT = 9003

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
      sHead(CustomPolicy(at => at.given_name.contains("test-user"))) {
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
  startServer(HOST, PORT)
}

object LoggingSupport {
  //start logging
  LoggingSystemFactory.start("example-server", "", "", actorSystem)
}

object AsyncSupport {
  implicit val actorSystem: typed.ActorSystem[SpawnProtocol.Command] = typed.ActorSystem(SpawnProtocol(), "")
  implicit val ec: ExecutionContext                                  = ExecutionContext.global
  implicit val mat: Materializer                                     = Materializer(actorSystem)
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
    def deleteEntity(entityId: Long, username: Option[String]): Nothing = ???

    def deleteEntity(entityId: Long): Nothing = ???
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

  object Policies {
    // #custom-policy-async
    //GET http://[host]:[port]/files?fileId=[fileId]
    val route: Route =
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

    //#realm-role-policy-usage
    val routeWithRealmRolePolicy: Route = sGet(RealmRolePolicy("admin")) {
      complete("OK")
    }
    //#realm-role-policy-usage

    //#client-role-policy-usage
    val routeWithClientRolePolicy: Route = sGet(ClientRolePolicy("accounts-admin")) {
      complete("OK")
    }
    //#client-role-policy-usage

    // #custom-policy-usage
    val routeWithCustomPolicy: Route = sPost(CustomPolicy(token => token.given_name.contains("test-user"))) {
      complete("OK")
    }
    // #custom-policy-usage

    // #permission-policy
    val routeWithPermissions: Route = sDelete(PermissionPolicy("delete", "account")) {
      complete("OK")
    }
    // #permission-policy
  }

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

  object DirectiveComposition {
    // #policy-expressions-right-way
    sGet(RealmRolePolicy("admin") & ClientRolePolicy("sales_admin"))
    // #policy-expressions-right-way

    // #directive-composition-anti-pattern
    sGet(RealmRolePolicy("admin")) & sGet(ClientRolePolicy("sales_admin"))
    // #directive-composition-anti-pattern
  }

  val routes: Route = ???
}

// #sample-http-app
object SampleHttpApp extends HttpApp with App {

  implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = typed.ActorSystem(SpawnProtocol(), "sample-http-app")
  implicit val ec: ExecutionContext                            = actorSystem.executionContext
  implicit val mat: Materializer                               = Materializer(actorSystem)

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

  private val host = "0.0.0.0"
  private val port = 9003

  startServer(host, port)
}
// #sample-http-app
