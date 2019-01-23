package csw.auth

import akka.actor.ActorSystem
import akka.http.scaladsl.server._
import akka.http.scaladsl.unmarshalling.GenericUnmarshallers
import akka.stream.ActorMaterializer
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import csw.aas.http.AuthorizationPolicy._
import csw.aas.http.SecurityDirectives
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.client.utils.LocationServerStatus
import csw.logging.client.scaladsl.LoggingSystemFactory
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.implicitConversions

object ExampleServer extends HttpApp with App with GenericUnmarshallers with PlayJsonSupport {

  import Async._

  LoggingSystemFactory.start("example-server", "", "", Async.actorSystem)

  //ensure location service is up
  LocationServerStatus.requireUpLocally(5.seconds)

  // #security-directive-usage
  //create location client
  private val locationService = HttpLocationServiceFactory.makeLocalClient

  private val directives = SecurityDirectives(locationService)

  import directives._
  // #security-directive-usage

  private val HOST = "0.0.0.0"
  private val PORT = 9003

  // #example-routes
  override protected def routes: Route = cors() {
    pathPrefix("person") {
      get {
        complete("OK")
      } ~
      // #secure-route-example
      sPost(
        // #realm-role-policy
        RealmRolePolicy("example-admin-role")
        // #realm-role-policy
      ) { _ ⇒
        complete("Person created OK")
      } ~
      // #secure-route-example
      sPut(
        // #resource-role-policy
        ResourceRolePolicy("person-role")
        // #resource-role-policy
      ) { _ ⇒
        complete("Person updated OK")
      } ~
      sPatch(ResourceRolePolicy("some-role")) { _ ⇒
        complete("Person updated OK")
      } ~
      sHead(
        // #custom-policy
        CustomPolicy(at ⇒ at.given_name.contains("test-user"))
        // #custom-policy
      ) { _ ⇒
        complete("Custom policy OK")
      } ~
      sDelete(
        // #permission-policy
        PermissionPolicy("delete", "person")
        // #permission-policy
      ) { _ ⇒
        complete("Permission policy OK")
      } ~
      sGet(
        // #empty-policy
        EmptyPolicy
        // #empty-policy
      ) { _ ⇒
        complete("Empty policy OK")
      }
    }
  }
  // #example-routes
  startServer(HOST, PORT)
}

private object Async {
  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val ec: ExecutionContext     = ExecutionContext.global
  implicit val mat: ActorMaterializer   = ActorMaterializer()

  implicit def resolveFuture[T](future: Future[T]): T = {
    Await.result(future, 5.seconds)
  }
}
