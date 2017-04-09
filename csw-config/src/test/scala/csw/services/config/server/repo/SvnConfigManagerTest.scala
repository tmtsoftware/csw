package csw.services.config.server.repo

import csw.services.config.scaladsl.{ConfigManager, ConfigManagerTest}
import csw.services.config.server.ServerWiring

class SvnConfigManagerTest extends ConfigManagerTest {
  private val wiring = new ServerWiring()
  override def configManager: ConfigManager = wiring.configManager
}
