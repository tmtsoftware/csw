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
import org.scalatest.OptionValues

class HttpServiceOnPublicNetworkTest extends HTTPLocationService with OptionValues {
  private val networkAutoDetectionOff = ConfigFactory.parseString("csw-networks.hostname.automatic = off")
  private val config                  = networkAutoDetectionOff.withFallback(ConfigFactory.load())
  private val system                  = ActorSystem(SpawnProtocol(), "test", config)
  import system.executionContext
  private val directives: SecurityDirectives = SecurityDirectives.authDisabled(config)

  override def beforeAll(): Unit = {
    super.beforeAll()
    System.clearProperty(NetworkType.Public.envKey)
  }

  override def afterAll(): Unit = {
    system.terminate()
    system.whenTerminated.await
    super.afterAll()
  }

  test("should bind config server using PUBLIC_INTERFACE_NAME env variable | CSW 97") {
    val network = Networks(NetworkType.Public.envKey)
    System.setProperty(NetworkType.Public.envKey, network.ipv4AddressWithInterfaceName._1)
    val _servicePort = 4005
    val serverWiring = ServerWiring.make(Some(_servicePort), system, directives)
    import serverWiring.{httpService, locationService}

    val (_, registrationResult) = httpService.registeredLazyBinding.await
    locationService.find(ConfigServiceConnection.value).futureValue.value.connection shouldBe ConfigServiceConnection.value

    val location = registrationResult.location
    location.uri.getHost shouldBe network.hostname
    location.connection shouldBe ConfigServiceConnection.value
    locationService.unregister(location.connection).futureValue
  }

  test("should not bind config server and throw exception when PUBLIC_INTERFACE_NAME env variable is not Set | CSW-97") {
    System.clearProperty(NetworkType.Public.envKey)
    val _servicePort = 4006
    val serverWiring = ServerWiring.make(Some(_servicePort), system, directives)
    import serverWiring._

    val networkInterfaceNotProvidedException = intercept[NetworkInterfaceNotProvided] {
      httpService.registeredLazyBinding.await
    }

    networkInterfaceNotProvidedException.getMessage shouldBe "PUBLIC_INTERFACE_NAME env variable is not set."
  }
}
