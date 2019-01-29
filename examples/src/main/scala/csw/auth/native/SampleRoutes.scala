package csw.auth.native

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{HttpApp, Route}
import csw.aas.http.AuthorizationPolicy.RealmRolePolicy
import spray.json.DefaultJsonProtocol._

import scala.concurrent.ExecutionContext

object SampleRoutes {

  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val ec: ExecutionContext     = actorSystem.dispatcher
  val securityDirectives                = csw.aas.http.SecurityDirectives()
  import securityDirectives._

  // #sample-routes
  var data: Set[String] = Set.empty

  val routes: Route =
    pathPrefix("data") {
      get { // e.g HTTP GET http://localhost:7000/data
        pathEndOrSingleSlash {
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
