package csw.services.config.server

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import csw.services.config.commons.ActorRuntime
import csw.services.config.scaladsl.ConfigManager
import csw.services.config.server.http.{ConfigServiceRoute, HttpService}
import csw.services.config.server.repo.{OversizeFileManager, SvnAdmin, SvnConfigManager}

class ServerWiring {
  val config: Config = ConfigFactory.load()
  val settings = new Settings(config)

  val actorSystem = ActorSystem("config-service", config)
  val actorRuntime = new ActorRuntime(actorSystem)

  val oversizeFileManager = new OversizeFileManager(settings)
  val svnAdmin = new SvnAdmin(settings)
  val configManager: ConfigManager = new SvnConfigManager(settings, oversizeFileManager, actorRuntime)

  val configServiceRoute = new ConfigServiceRoute(configManager, actorRuntime)
  val httpService = new HttpService(configServiceRoute, settings, actorRuntime)
}
