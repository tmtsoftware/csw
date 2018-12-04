package csw.auth

import akka.actor.ActorSystem
import akka.http.scaladsl.server._
import akka.http.scaladsl.unmarshalling.GenericUnmarshallers
import akka.stream.ActorMaterializer
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import csw.aas.core.deployment.{AuthConfig, AuthServiceLocation}
import csw.aas.core.token.TokenFactory
import csw.aas.http.AuthorizationPolicy._
import csw.aas.http.{Authentication, SecurityDirectives}
import csw.location.api.models.RegistrationResult
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.client.utils.LocationServerStatus
import csw.logging.scaladsl.LoggingSystemFactory
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.implicitConversions

object ExampleServer extends HttpApp with App with GenericUnmarshallers with PlayJsonSupport {

  import Async._

  LoggingSystemFactory.start("example-http-server", "", "", Async.actorSystem)

  //ensure location service is up
  LocationServerStatus.requireUpLocally(5.seconds)

  //create location client
  private val locationService = HttpLocationServiceFactory.makeLocalClient

  //create in instance of authLocation. This can be used to resolve or register auth service
  val authLocation = AuthServiceLocation(locationService)

  //register the auth service, since automatic registration is not in place yet
  val registrationResult: RegistrationResult = authLocation.register()

  //create new authConfig using resolved auth server location
  private val authConfig = AuthConfig.loadFromAppConfig(authLocation.resolve)

  private val authentication = Authentication(TokenFactory(authConfig))
  private val directives     = SecurityDirectives(authentication, authConfig)

  import directives._
  private val HOST = "localhost"
  private val PORT = 9003

  override protected def routes: Route = cors() {
    pathPrefix("person") {
      get {
        complete("OK")
      } ~
      sPost(RealmRolePolicy("example-admin-role")) { _ ⇒
        complete("Person created OK")
      } ~
      sDelete(PermissionPolicy("delete", "person")) { _ ⇒
        complete("Person deleted OK")
      } ~
      sPut(ResourceRolePolicy("person-role")) { _ ⇒
        complete("Person updated OK")
      } ~
      sHead(CustomPolicy(at ⇒ at.given_name.contains("test-user"))) { _ ⇒
        complete("Custom policy OK")
      }
    }
  }
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
