package csw.services.config.internal

import com.typesafe.config.{Config, ConfigFactory}
import csw.services.config.scaladsl.ConfigManager
import csw.services.config.server.ConfigServiceApp

object Wiring {
  val config: Config = ConfigFactory.load()
  val settings = new Settings(config)
  val oversizeFileManager = new OversizeFileManager(settings)
  val svnAdmin = new SvnAdmin(settings)
  val configManager: ConfigManager = new SvnConfigManager(settings, oversizeFileManager)
  val configServiceApp = new ConfigServiceApp(configManager)
}
