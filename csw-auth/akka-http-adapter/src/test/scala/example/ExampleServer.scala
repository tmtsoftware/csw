package example
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.http.scaladsl.unmarshalling.GenericUnmarshallers
import csw.auth.adapters.akka.http.{Authentication, SecurityDirectives}
import csw.auth.core.token.TokenFactory
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

object ExampleServer extends HttpApp with App with GenericUnmarshallers with PlayJsonSupport {

  private val directives = SecurityDirectives(new Authentication(new TokenFactory))

  import directives._
  private val HOST = "localhost"
  private val PORT = 9002

  override protected def routes: Route = {
    path("config") {
      get {
        permission("read", "config") {
          complete("OK")
        }
      } ~ put {
        permission("write", "config") {
          complete("OK")
        }
      } ~ post {
        entity(as[Person]) { person =>
          customPolicy(
            at =>
              person.country == "US"
              && at.email.getOrElse("").endsWith("gmail.com")
          ) {
            complete("OK")
          }
        }
      } ~
      patch {
        resourceRole("example-service-admin") {
          complete("OK")
        }
      }
    }
  }

  startServer(HOST, PORT)
}
