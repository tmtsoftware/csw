package csw.auth

import akka.http.scaladsl.server._
import akka.http.scaladsl.unmarshalling.GenericUnmarshallers
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import csw.aas.core.deployment.AuthConfig
import csw.aas.core.token.TokenFactory
import csw.aas.http.AuthorizationPolicy._
import csw.aas.http.{Authentication, SecurityDirectives}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

object ExampleServer extends HttpApp with App with GenericUnmarshallers with PlayJsonSupport {

  private val authConfig     = AuthConfig.loadFromAppConfig
  private val tokenFactory   = new TokenFactory(authConfig)
  private val authentication = new Authentication(tokenFactory)
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
