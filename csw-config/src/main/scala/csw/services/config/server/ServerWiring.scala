package csw.services.config.server

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import csw.services.config.api.scaladsl.ConfigManager
import csw.services.config.server.http.{ConfigServiceRoute, HttpService}
import csw.services.config.server.repo.{FileOps, OversizeFileManager, SvnAdmin, SvnConfigManager}
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}

class ServerWiring {
  lazy val config: Config = ConfigFactory.load()
  lazy val settings = new Settings(config)

  lazy val actorSystem = ActorSystem("config-service", config)
  lazy val actorRuntime = new ActorRuntime(actorSystem, settings)
  lazy val fileOps = new FileOps(actorRuntime.blockingIoDispatcher)

  lazy val oversizeFileManager = new OversizeFileManager(settings, fileOps)
  lazy val svnAdmin = new SvnAdmin(settings)
  lazy val configManager: ConfigManager = new SvnConfigManager(settings, oversizeFileManager, actorRuntime)

  lazy val locationService: LocationService = LocationServiceFactory.make()

  lazy val configServiceRoute = new ConfigServiceRoute(configManager, actorRuntime)
  lazy val httpService = new HttpService(locationService, configServiceRoute, settings, actorRuntime)
}
