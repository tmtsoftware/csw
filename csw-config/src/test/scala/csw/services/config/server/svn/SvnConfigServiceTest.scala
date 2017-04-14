package csw.services.config.server.svn

import csw.services.config.api.scaladsl.{ConfigService, ConfigServiceTest}
import csw.services.config.server.ServerWiring

class SvnConfigServiceTest extends ConfigServiceTest {
  override val configService: ConfigService = new ServerWiring().configService
}
