package csw.services.config.client.scaladsl

import csw.services.config.api.scaladsl.ConfigService
import csw.services.config.client.internal.ActorRuntime
import csw.services.config.server.commons.TestFutureExtension.RichFuture
import csw.services.config.server.{ConfigServiceTest, ServerWiring}
import csw.services.location.commons.ClusterAwareSettings
import csw.services.location.scaladsl.LocationServiceFactory

class ConfigClientTest extends ConfigServiceTest {

  private val clientLocationService = LocationServiceFactory.withSettings(ClusterAwareSettings.onPort(3552))

  private val httpService = ServerWiring.make(ClusterAwareSettings.joinLocal(3552)).httpService

  private val actorRuntime = new ActorRuntime()
  import actorRuntime._

  override val configService: ConfigService = ConfigClientFactory.adminApi(actorSystem, clientLocationService)

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    httpService.registeredLazyBinding.await
  }

  override protected def afterAll(): Unit = {
    actorSystem.terminate().await
    httpService.shutdown().await
    clientLocationService.shutdown().await
    super.afterAll()
  }
}
