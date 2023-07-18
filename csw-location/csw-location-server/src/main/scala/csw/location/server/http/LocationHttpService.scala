/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.server.http

import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.server.Directives.handleRejections
import org.apache.pekko.http.scaladsl.server.{RejectionHandler, Route}
import org.apache.pekko.http.cors.scaladsl.CorsDirectives._
import csw.location.server.internal.{ActorRuntime, Settings}

import scala.concurrent.Future

class LocationHttpService(locationRoutes: Route, actorRuntime: ActorRuntime, settings: Settings) {

  implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = actorRuntime.actorSystem

  private def applicationRoute: Route = {
    val rejectionHandler = corsRejectionHandler.withFallback(RejectionHandler.default)
    cors() {
      handleRejections(rejectionHandler) {
        locationRoutes
      }
    }
  }

  def start(httpBindHost: String = "127.0.0.1"): Future[Http.ServerBinding] = {
    Http().newServerAt(httpBindHost, settings.httpPort).bind(applicationRoute)
  }
}
