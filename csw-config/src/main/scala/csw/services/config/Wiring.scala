package csw.services.config

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import csw.services.config.repo.{OversizeFileManager, SvnAdmin, SvnConfigManager}
import csw.services.config.scaladsl.ConfigManager
import csw.services.config.server.ConfigServiceApp

class Wiring {
  val config: Config = ConfigFactory.load()
  val actorSystem = ActorSystem("config-service", config)
  val settings = new Settings(config)
  val oversizeFileManager = new OversizeFileManager(settings)
  val svnAdmin = new SvnAdmin(settings)
  val configManager: ConfigManager = new SvnConfigManager(settings, oversizeFileManager)
  val configServiceApp = new ConfigServiceApp(configManager, actorSystem)
}
