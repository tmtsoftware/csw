/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package example.auth.installed

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route
import com.typesafe.config.{Config, ConfigFactory}
import csw.aas.http.AuthorizationPolicy.RealmRolePolicy
import csw.aas.http.SecurityDirectives
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.ExecutionContext

class SampleRoutes(securityDirectives: SecurityDirectives)(implicit actorSystem: ActorSystem[_]) {

  import actorSystem.executionContext
  import securityDirectives._

  // #sample-routes
  var data: Set[String] = Set.empty

  val routes: Route =
    pathPrefix("data") {
      get {                    // un-protected route for reading data
        pathEndOrSingleSlash { // e.g HTTP GET http://localhost:7000/data
          complete(data.toJson)
        }
      } ~ sPost(RealmRolePolicy("admin")) { // only users with 'admin' role is allowed for this route
        parameter("value") { value =>       // e.g POST GET localhost:7000/data?value=abc
          data = data + value
          complete(StatusCodes.OK)
        }
      }
    }
  // #sample-routes
}

object SampleServer {
  def main(args: Array[String]): Unit = {
    implicit val actorSystem: ActorSystem[_] = ActorSystem(Behaviors.empty, "test")
    implicit val ec: ExecutionContext        = actorSystem.executionContext

    val config: Config = ConfigFactory.parseString("""
                                                     | auth-config {
                                                     |  realm = TMT
                                                     |  client-id = tmt-backend-app
                                                     | }
      """.stripMargin)

    val locationService: LocationService       = HttpLocationServiceFactory.makeLocalClient
    val securityDirectives: SecurityDirectives = SecurityDirectives(config, locationService)
    val sampleRoutes                           = new SampleRoutes(securityDirectives)

    Http().newServerAt("localhost", 7000).bind(sampleRoutes.routes)
  }
}
