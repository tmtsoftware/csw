/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.server.internal

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.server.Route
import com.typesafe.config.{Config, ConfigFactory}
import csw.aas.http.SecurityDirectives
import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.scaladsl.LocationService
import csw.location.server.commons.{ClusterAwareSettings, ClusterSettings}
import csw.location.server.http.{LocationHttpService, LocationRequestHandler, LocationStreamRequestHandler}
import msocket.http.RouteFactory
import msocket.http.post.PostRouteFactory
import msocket.http.ws.WebsocketRouteFactory
import msocket.jvm.metrics.LabelExtractor

// $COVERAGE-OFF$
private[csw] class ServerWiring(enableAuth: Boolean) extends LocationServiceCodecs {
  lazy val config: Config                                           = ConfigFactory.load()
  lazy val settings                                                 = new Settings(config)
  lazy val clusterSettings: ClusterSettings                         = ClusterAwareSettings.onPort(settings.clusterPort)
  implicit lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = clusterSettings.system
  lazy val actorRuntime                                             = new ActorRuntime(actorSystem)
  import actorSystem.executionContext
  lazy val locationService: LocationService = LocationServiceFactory.make(clusterSettings)

  lazy val securityDirectives: SecurityDirectives = SecurityDirectives(config, locationService, enableAuth)

  private lazy val postHandler      = new LocationRequestHandler(locationService, securityDirectives)
  private lazy val websocketHandler = new LocationStreamRequestHandler(locationService)

  import LabelExtractor.Implicits.default
  lazy val locationRoutes: Route = RouteFactory.combine(metricsEnabled = false)(
    new PostRouteFactory("post-endpoint", postHandler),
    new WebsocketRouteFactory("websocket-endpoint", websocketHandler)
  )

  lazy val locationHttpService = new LocationHttpService(locationRoutes, actorRuntime, settings)
}

private[csw] object ServerWiring {

  def make(maybeClusterPort: Option[Int], enableAuth: Boolean): ServerWiring =
    new ServerWiring(enableAuth) {
      override lazy val settings: Settings = new Settings(config) {
        override val clusterPort: Int = maybeClusterPort.getOrElse(super.clusterPort)
      }
    }

  def make(maybeClusterPort: Option[Int], mayBeHttpPort: Option[Int], enableAuth: Boolean): ServerWiring =
    new ServerWiring(enableAuth) {
      override lazy val settings: Settings = {
        new Settings(config) {
          override val clusterPort: Int = maybeClusterPort.getOrElse(super.clusterPort)
          override val httpPort: Int    = mayBeHttpPort.getOrElse(super.httpPort)
        }
      }
    }

  def make(_clusterSettings: ClusterSettings, enableAuth: Boolean): ServerWiring =
    new ServerWiring(enableAuth) {
      override lazy val clusterSettings: ClusterSettings = _clusterSettings
    }

}
// $COVERAGE-ON$
