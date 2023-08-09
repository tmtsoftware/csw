/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package example.auth

import org.apache.pekko.actor.typed
import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.http.scaladsl.server.RouteConcatenation._enhanceRouteWithConcatenation
import org.apache.pekko.http.scaladsl.server.directives.MethodDirectives.get
import org.apache.pekko.http.scaladsl.server.directives.PathDirectives.pathPrefix
import org.apache.pekko.http.scaladsl.server.directives.RouteDirectives.complete
import csw.aas.http.AuthorizationPolicy.RealmRolePolicy
import csw.aas.http.SecurityDirectives
import csw.location.client.scaladsl.HttpLocationServiceFactory

import scala.concurrent.ExecutionContext

// #sample-http-app
object SampleHttpApp extends App {

  implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = typed.ActorSystem(SpawnProtocol(), "sample-http-app")
  implicit val ec: ExecutionContext                            = actorSystem.executionContext
  private val config                                           = actorSystem.settings.config

  val locationService = HttpLocationServiceFactory.makeLocalClient
  val directives      = SecurityDirectives(config, locationService)
  import directives._

  def routes: Route =
    pathPrefix("api") {
      get {
        complete("SUCCESS")
      } ~
      sPost(RealmRolePolicy("admin")) {
        complete("SUCCESS")
      }
    }

  private val host = "0.0.0.0"
  private val port = 9003

  Http().newServerAt(host, port).bind(routes)
}
// #sample-http-app
