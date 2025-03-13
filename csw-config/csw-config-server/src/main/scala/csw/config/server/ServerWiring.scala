/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.config.server

import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import com.typesafe.config.{Config, ConfigFactory}
import csw.aas.http.SecurityDirectives
import csw.config.server.files.*
import csw.config.server.http.{ConfigHandlers, ConfigServiceRoute, HttpService}
import csw.config.server.svn.{SvnConfigServiceFactory, SvnRepo}
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory

/**
 * Server configuration
 */
class ServerWiring {
  lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "config-server")
  lazy val config: Config                                  = actorSystem.settings.config
  lazy val settings                                        = new Settings(config)

  final lazy val actorRuntime = new ActorRuntime(actorSystem, settings)
  import actorRuntime._

  lazy val annexFileRepo    = new AnnexFileRepo(actorRuntime.blockingIoDispatcher)
  lazy val annexFileService = new AnnexFileService(settings, annexFileRepo, actorRuntime)

  lazy val svnRepo              = new SvnRepo(settings.`svn-user-name`, settings, actorRuntime.blockingIoDispatcher)
  lazy val configServiceFactory = new SvnConfigServiceFactory(actorRuntime, annexFileService)

  lazy val locationService: LocationService =
    HttpLocationServiceFactory.makeLocalClient(actorSystem)

  lazy val configHandlers                         = new ConfigHandlers
  lazy val securityDirectives: SecurityDirectives = SecurityDirectives(config, locationService)
  final lazy val configServiceRoute =
    new ConfigServiceRoute(configServiceFactory, actorRuntime, configHandlers, securityDirectives)

  lazy val httpService: HttpService = new HttpService(locationService, configServiceRoute, settings, actorRuntime)
}

private[csw] object ServerWiring {

  def make(maybePort: Option[Int]): ServerWiring =
    new ServerWiring {
      override lazy val settings: Settings = new Settings(config) {
        override val `service-port`: Int = maybePort.getOrElse(super.`service-port`)
      }
    }

  def make(_config: Config): ServerWiring =
    new ServerWiring {
      override lazy val config: Config = _config.withFallback(ConfigFactory.load())
    }

  def make(_locationService: LocationService, _securityDirectives: SecurityDirectives): ServerWiring =
    new ServerWiring {
      override lazy val locationService: LocationService       = _locationService
      override lazy val securityDirectives: SecurityDirectives = _securityDirectives
    }

  def make(_securityDirectives: SecurityDirectives): ServerWiring =
    new ServerWiring {
      override lazy val securityDirectives: SecurityDirectives = _securityDirectives
    }
}
