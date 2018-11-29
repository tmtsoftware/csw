package csw.auth

import akka.http.scaladsl.server._
import akka.http.scaladsl.unmarshalling.GenericUnmarshallers
import csw.auth.adapters.akka.http.AuthorizationPolicy._
import csw.auth.adapters.akka.http.{Authentication, SecurityDirectives}
import csw.auth.core.token.TokenFactory
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

object ExampleServer extends HttpApp with App with GenericUnmarshallers with PlayJsonSupport {

  private val directives = SecurityDirectives(new Authentication(new TokenFactory))

  import directives._
  private val HOST = "localhost"
  private val PORT = 9003

  override protected def routes: Route = pathPrefix("person") {
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

  startServer(HOST, PORT)
}
