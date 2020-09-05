package csw.location.server

import csw.aas.core.commons.AASConnection
import csw.location.api.models.{HttpRegistration, NetworkType}
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.server.cli.ArgsParser
import csw.network.utils.{Networks, SocketUtils}
import msocket.http.HttpError
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.DurationInt
class MainTest extends AnyFunSuiteLike with Matchers with ScalaFutures {
  private val httpPort                          = 7654
  private implicit val patience: PatienceConfig = PatienceConfig(5.seconds, 100.millis)

  test("when publicNetwork option NOT given, should bind to 127.0.0.1 with auth DISABLED | CSW-96, CSW-89") {
    val args              = Array("--clusterPort", s"${SocketUtils.getFreePort}")
    val options           = new ArgsParser("csw-location-server").parse(args.toList).get
    val `127.0.0.1`       = "127.0.0.1"
    val (binding, wiring) = Main.start(startLogging = false, options = options)

    //assert location server running at 127.0.0.1
    binding.localAddress.getAddress.getHostAddress shouldBe `127.0.0.1`
    SocketUtils.isAddressInUse(`127.0.0.1`, httpPort) shouldBe true

    // Can access protected resource of location server running at 127.0.0.1, as auth is disabled
    val httpClient: LocationService = HttpLocationServiceFactory.makeLocalClient(wiring.actorSystem)
    httpClient.unregisterAll().futureValue

    //Clean up
    binding.terminate(2.seconds).futureValue
    wiring.actorRuntime.shutdown().futureValue
  }

  test("when publicNetwork option is given, should bind to Public Network IP with auth ENABLED | CSW-96, CSW-89") {
    val args     = Array("--clusterPort", s"${SocketUtils.getFreePort}", "--publicNetwork")
    val options  = new ArgsParser("csw-location-server").parse(args.toList).get
    val hostname = Networks(NetworkType.Public.envKey).hostname

    val (binding, wiring) = Main.start(startLogging = false, options = options)
    //AAS location is registered here to make sure, it does not give AASResolution Failed error
    wiring.locationService.register(HttpRegistration(AASConnection.value, SocketUtils.getFreePort, "auth")).futureValue

    //assert location server running at hostname
    binding.localAddress.getAddress.getHostAddress shouldBe hostname
    SocketUtils.isAddressInUse(hostname, httpPort) shouldBe true

    // Can not access protected resource of location server running at hostname, as auth is enabled
    val httpClient: LocationService = HttpLocationServiceFactory.make(hostname)(wiring.actorSystem)
    val exception                   = intercept[Exception](httpClient.unregisterAll().futureValue)
    exception.getCause shouldBe a[HttpError]
    exception.getCause.asInstanceOf[HttpError].statusCode shouldBe 401

    //Clean up
    binding.terminate(2.seconds).futureValue
    wiring.actorRuntime.shutdown().futureValue
  }
}
