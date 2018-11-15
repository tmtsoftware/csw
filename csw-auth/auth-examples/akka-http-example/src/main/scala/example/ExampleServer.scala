package example
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.http.scaladsl.unmarshalling.GenericUnmarshallers
import csw.auth.SecurityDirectives._
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import julienrf.json.derived
import play.api.libs.json.{__, OFormat}

object ExampleServer extends HttpApp with App with GenericUnmarshallers with PlayJsonSupport {

  private val HOST = "localhost"
  private val PORT = 9002

  case class Person(name: String, city: String, country: String)
  object Person {
    implicit val personFormat: OFormat[Person] = derived.flat.oformat((__ \ "type").format[String])
  }

  override protected def routes: Route = {
    path("config") {
      get {
        permission("read", "config") {
          complete("OK")
        }
      } ~ post {
        entity(as[Person]) { person =>
          customPolicy(person.country == "US") {
            complete("OK")
          }
        }
      }
    }
  }

  startServer(HOST, PORT)
}
