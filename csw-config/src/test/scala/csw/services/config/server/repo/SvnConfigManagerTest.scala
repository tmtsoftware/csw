package csw.services.config.server.repo

import csw.services.config.api.scaladsl.{ConfigManager, ConfigManagerTest}
import csw.services.config.server.ServerWiring

class SvnConfigManagerTest extends ConfigManagerTest {
  override lazy val serverWiring = new ServerWiring
  override val configManager: ConfigManager = serverWiring.configManager
}
