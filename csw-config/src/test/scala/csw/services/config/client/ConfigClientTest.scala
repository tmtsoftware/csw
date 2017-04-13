package csw.services.config.client

import csw.services.config.api.commons.TestFutureExtension.RichFuture
import csw.services.config.api.scaladsl.{ConfigService, ConfigServiceTest}
import csw.services.config.server.ServerWiring
import csw.services.location.commons.ClusterSettings
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}

class CustomServerWiring extends ServerWiring {
  override lazy val locationService: LocationService = {
    LocationServiceFactory.withSettings(ClusterSettings().onPort(3552))
  }
}

class CustomClientWiring extends ClientWiring {
    override lazy val locationService: LocationService = {
      val clientLocationService = LocationServiceFactory.withSettings(ClusterSettings().joinLocal(3552))
      Thread.sleep(2000)
      clientLocationService
    }
}

class ConfigClientTest extends ConfigServiceTest {

  override lazy val serverWiring = new CustomServerWiring
  serverWiring.httpService.lazyBinding.await

  lazy val clientWiring = new CustomClientWiring

  override val configManager: ConfigService = clientWiring.configService

  override protected def afterAll(): Unit = {
    serverWiring.httpService.shutdown().await
    clientWiring.actorSystem.terminate().await
  }
}
