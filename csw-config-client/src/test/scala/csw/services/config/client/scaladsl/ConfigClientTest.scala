package csw.services.config.client.scaladsl

import csw.services.config.api.scaladsl.ConfigService
import csw.services.config.client.internal.ActorRuntime
import csw.services.config.server.commons.TestFutureExtension.RichFuture
import csw.services.config.server.{ConfigServiceTest, ServerWiring}
import csw.services.location.commons.ClusterSettings
import csw.services.location.scaladsl.LocationServiceFactory

class ConfigClientTest extends ConfigServiceTest {

  private val locationService       = LocationServiceFactory.withSettings(ClusterSettings().onPort(3552))
  private val clientLocationService = LocationServiceFactory.withSettings(ClusterSettings().joinLocal(3552))

  private val httpService = ServerWiring.make(locationService).httpService

  private val actorRuntime = new ActorRuntime()
  import actorRuntime._

  override val configService: ConfigService = ConfigClientFactory.make(actorSystem, clientLocationService)

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
