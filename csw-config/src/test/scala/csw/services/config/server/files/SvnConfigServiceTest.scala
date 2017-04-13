package csw.services.config.server.files

import csw.services.config.api.scaladsl.{ConfigService, ConfigServiceTest}
import csw.services.config.server.ServerWiring

class SvnConfigServiceTest extends ConfigServiceTest {
  override lazy val serverWiring = new ServerWiring
  override val configService: ConfigService = serverWiring.configService
}
