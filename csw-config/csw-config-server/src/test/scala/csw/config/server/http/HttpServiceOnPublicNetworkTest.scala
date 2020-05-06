package csw.config.server.http

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import com.typesafe.config.ConfigFactory
import csw.aas.http.SecurityDirectives
import csw.config.server.ServerWiring
import csw.config.server.commons.ConfigServiceConnection
import csw.config.server.commons.TestFutureExtension.RichFuture
import csw.location.api.models.NetworkType
import csw.location.server.http.HTTPLocationService
import csw.network.utils.Networks
import csw.network.utils.exceptions.NetworkInterfaceNotProvided

class HttpServiceOnPublicNetworkTest extends HTTPLocationService {

  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")

  override def beforeAll(): Unit = {
    super.beforeAll()
    System.clearProperty(NetworkType.Public.envKey)
  }

  override def afterAll(): Unit = {
    system.terminate()
    system.whenTerminated.await
    super.afterAll()
  }

  test("should bind config server when PUBLIC_INTERFACE_NAME env variable is Set") {
    System.setProperty(NetworkType.Public.envKey, "en0")
    val _servicePort = 4005
    lazy val config  = ConfigFactory.parseString("csw-networks.hostname.automatic = off")
    val serverWiring =
      ServerWiring.make(
        Some(_servicePort),
        config,
        SecurityDirectives.authDisabled(ConfigFactory.load())(system.executionContext)
      )
    import serverWiring._

    val (_, registrationResult) = httpService.registeredLazyBinding.await
    locationService.find(ConfigServiceConnection.value).await.get.connection shouldBe ConfigServiceConnection.value

    val location = registrationResult.location
    location.uri.getHost shouldBe Networks(NetworkType.Public.envKey).hostname
    location.connection shouldBe ConfigServiceConnection.value
    locationService.unregister(location.connection).await

  }

  test("should not bind config server and throw exception when PUBLIC_INTERFACE_NAME env variable is not Set") {
    System.clearProperty(NetworkType.Public.envKey)
    val _servicePort = 4006
    lazy val config  = ConfigFactory.parseString("csw-networks.hostname.automatic = off")
    val serverWiring =
      ServerWiring.make(
        Some(_servicePort),
        config,
        SecurityDirectives.authDisabled(ConfigFactory.load())(system.executionContext)
      )
    import serverWiring._

    val networkInterfaceNotProvidedException = intercept[NetworkInterfaceNotProvided] {
      httpService.registeredLazyBinding.await
    }

    networkInterfaceNotProvidedException.getMessage shouldBe "PUBLIC_INTERFACE_NAME env variable is not set."
  }

}
