package csw.services.config.internal

import com.typesafe.config.{Config, ConfigFactory}
import csw.services.config.scaladsl.ConfigManager

object Wiring {
  val config: Config = ConfigFactory.load()
  val settings = new Settings(config)
  val largeFileManager = new LargeFileManager(settings)
  val svnAdmin = new SvnAdmin(settings)
  val configManager: ConfigManager = new SvnConfigManager(settings, largeFileManager)
}
