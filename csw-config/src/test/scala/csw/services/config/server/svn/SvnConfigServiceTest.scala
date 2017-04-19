package csw.services.config.server.svn

import csw.services.config.api.commons.TestFutureExtension.RichFuture
import csw.services.config.api.scaladsl.{ConfigService, ConfigServiceTest}
import csw.services.config.server.ServerWiring

class SvnConfigServiceTest extends ConfigServiceTest {
  val serverWiring                          = new ServerWiring()
  override val configService: ConfigService = serverWiring.configService

  override protected def afterAll(): Unit = {
    serverWiring.actorSystem.terminate().await
    super.afterAll()
  }
}
