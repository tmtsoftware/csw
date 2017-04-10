package csw.services.config.client

import csw.services.config.api.commons.TestFutureExtension.RichFuture
import csw.services.config.api.scaladsl.{ConfigManager, ConfigManagerTest}
import csw.services.config.server.ServerWiring
import org.scalatest.BeforeAndAfterAll

class ConfigClientTest(removeMe: Int) extends ConfigManagerTest with BeforeAndAfterAll {

  private val serverWiring = new ServerWiring
  private val clientWiring = new ClientWiring

  override protected def beforeAll(): Unit = {
    serverWiring.httpService.lazyBinding.await
  }

  override protected def afterAll(): Unit = {
    serverWiring.httpService.shutdown().await
  }

  override val configManager: ConfigManager = clientWiring.configManager
}
