package example
import akka.http.scaladsl.server._
import akka.http.scaladsl.unmarshalling.GenericUnmarshallers
import csw.auth.adapters.akka.http.AuthorizationPolicy.{CustomPolicy, ResourceRolePolicy}
import csw.auth.adapters.akka.http.{Authentication, SecurityDirectives}
import csw.auth.core.token.TokenFactory
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

object ExampleServer extends HttpApp with App with GenericUnmarshallers with PlayJsonSupport {

  private val directives = SecurityDirectives(new Authentication(new TokenFactory))

  import directives._
  private val HOST = "localhost"
  private val PORT = 9003

  override protected def routes: Route = path("config") {
    head {
      complete("HEAD OK")
    } ~
    sPost(ResourceRolePolicy("admin")) { _ =>
      complete("POST OK")
    } ~
    post {
      complete("UNAUTHORIZED POST")
    } ~
    get {
      complete("GET OK")
    } ~
    parameter('subsystem) { subsystem =>
      sGet(CustomPolicy(token => token.hasRealmRole(s"${subsystem}_admin"))) { _ =>
        //your route code goes here
        complete("SUCCESS")
      }
    }
  }

  startServer(HOST, PORT)
}
