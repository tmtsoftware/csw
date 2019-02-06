package csw.auth.native

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{HttpApp, Route}
import com.typesafe.config.ConfigFactory
import csw.aas.http.AuthorizationPolicy.RealmRolePolicy
import spray.json.DefaultJsonProtocol._

import scala.concurrent.ExecutionContext

object SampleRoutes {

  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val ec: ExecutionContext     = actorSystem.dispatcher
  val securityDirectives =
    csw.aas.http.SecurityDirectives(ConfigFactory.parseString("""
      | auth-config {
      |  realm = TMT
      |  client-id = demo-server
      |  auth-server-url = "http://10.131.124.57:8081/auth"
      | }
    """.stripMargin))
  import securityDirectives._

  // #sample-routes
  var data: Set[String] = Set.empty

  val routes: Route =
    pathPrefix("data") {
      get { // un-protected route for reading data
        pathEndOrSingleSlash { // e.g HTTP GET http://localhost:7000/data
          complete(data)
        }
      } ~ sPost(RealmRolePolicy("admin")) { // only users with 'admin' role is allowed for this route
        parameter("value") { value => // e.g POST GET localhost:7000/data?value=abc
          data = data + value
          complete(StatusCodes.OK)
        }
      }
    }
  // #sample-routes
}

object SampleServer extends HttpApp with App {
  override protected def routes: Route = SampleRoutes.routes
  startServer("localhost", 7000)
}
