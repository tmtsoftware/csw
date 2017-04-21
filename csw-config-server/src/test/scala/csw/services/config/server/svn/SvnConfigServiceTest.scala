package csw.services.config.server.svn

import csw.services.config.api.scaladsl.ConfigService
import csw.services.config.server.commons.TestFutureExtension.RichFuture
import csw.services.config.server.{ConfigServiceTest, ServerWiring}

class SvnConfigServiceTest extends ConfigServiceTest {
  val serverWiring                          = new ServerWiring()
  override val configService: ConfigService = serverWiring.configService

  override protected def afterAll(): Unit = {
    serverWiring.actorSystem.terminate().await
    super.afterAll()
  }
}
