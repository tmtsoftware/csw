/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.aas.http

import org.apache.pekko.actor.typed
import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route
import csw.aas.http.AuthorizationPolicy.RealmRolePolicy
import csw.location.api.scaladsl.LocationService

import scala.concurrent.{ExecutionContext, Future}

class TestServer(locationService: LocationService) {
  implicit lazy val actorSystem: typed.ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
  implicit lazy val ec: ExecutionContext                                  = actorSystem.executionContext
  private lazy val config                                                 = actorSystem.settings.config
  private lazy val securityDirectives                                     = SecurityDirectives(config, locationService)
  import securityDirectives._

  val routes: Route = get {
    complete("OK")
  } ~ sPost(RealmRolePolicy("admin")) {
    complete("OK")
  }

  def start(testServerPort: Int): Future[Http.ServerBinding] = {
    Http().newServerAt("0.0.0.0", testServerPort).bind(routes)
  }
}
