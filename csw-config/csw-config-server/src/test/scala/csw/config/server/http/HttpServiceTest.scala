package csw.config.server.http

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.aas.core.commons.AASConnection
import csw.config.server.ServerWiring
import csw.config.server.commons.ConfigServiceConnection
import csw.config.server.commons.TestFutureExtension.RichFuture
import csw.location.api.exceptions.OtherLocationIsRegistered
import csw.location.api.models
import csw.location.api.models.NetworkType
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.server.http.HTTPLocationService
import csw.network.utils.Networks

import scala.util.control.NonFatal

class HttpServiceTest extends HTTPLocationService {

  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
  private val testLocationService: LocationService        = HttpLocationServiceFactory.makeLocalClient

  //register AAS with location service
  private val AASPort = 8080

  override def beforeAll(): Unit = {
    super.beforeAll()
    testLocationService.register(models.HttpRegistration(AASConnection.value, AASPort, "auth")).await
  }

  override def afterAll(): Unit = {
    system.terminate()
    system.whenTerminated.await
    super.afterAll()
  }

  //CSW-97
  /*
   * Tests can be run of different OS and each OS follow its own naming convention for interface names.
   * e.g. for Mac OS  interface names are like en0, en1 ,etc. For Linux they are eth0, eth1, etc. and so on.
   * Hence, it is not feasible to set and use env variable - NetworkType.Public.envKey and NetworkType.Private.envKey
   * in tests, as they are machine dependent.
   * Instead, a config property `csw-networks.hostname.automatic` is enabled in test scope to automatically detect
   * appropriate interface and hostname, which means Networks().hostname and Networks(NetworkType.Public.envKey)
   * .hostname will be same in tests.
   */
  private val hostname: String = Networks(NetworkType.Public.envKey).hostname

  test("should bind the http server and register it with location service | CSW-97") {
    val _servicePort = 4005
    val serverWiring = ServerWiring.make(Some(_servicePort))
    import serverWiring._
    val (_, registrationResult) = httpService.registeredLazyBinding.await
    locationService.find(ConfigServiceConnection.value).await.get.connection shouldBe ConfigServiceConnection.value

    val location = registrationResult.location
    location.uri.getHost shouldBe hostname
    location.uri.getPort shouldBe _servicePort
    location.connection shouldBe ConfigServiceConnection.value
    actorRuntime.shutdown().await
  }

  test("should not register with location service if server binding fails | CSW-97") {
    val _servicePort = 3553 // Location Service runs on this port
    val serverWiring = ServerWiring.make(Some(_servicePort))
    val expectedMessage = s"Bind failed because of java.net.BindException: [/${hostname}:${_servicePort}] Address " +
      s"already in use"
    import serverWiring._

    val bindException = intercept[Exception] { httpService.registeredLazyBinding.await }

    bindException.getMessage shouldBe expectedMessage
    testLocationService.find(ConfigServiceConnection.value).await shouldBe None

  }

  test("should not start server if registration with location service fails") {
    val _someOtherLocationPort = 21212
    val _servicePort           = 4007
    val serverWiring           = ServerWiring.make(Some(_servicePort))
    import serverWiring._
    locationService.register(models.HttpRegistration(ConfigServiceConnection.value, _someOtherLocationPort, "")).await

    locationService.find(ConfigServiceConnection.value).await.get.connection shouldBe ConfigServiceConnection.value

    a[OtherLocationIsRegistered] shouldBe thrownBy(httpService.registeredLazyBinding.await)

    //TODO: Find a way to assert server is not bounded
    try actorRuntime.shutdown().await
    catch {
      case NonFatal(_) =>
    }
  }
}
