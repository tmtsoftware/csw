package csw.location.server

import csw.aas.core.commons.AASConnection
import csw.location.api.models.{HttpRegistration, NetworkType}
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.network.utils.{Networks, SocketUtils}
import msocket.impl.HttpError
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.DurationInt
class MainTest extends AnyFunSuiteLike with Matchers with BeforeAndAfterAll with ScalaFutures {
  private val clusterSeedIp             = Networks().hostname
  private val httpPort                  = 7654
  implicit val patience: PatienceConfig = PatienceConfig(5.seconds, 100.millis)

  override def afterAll(): Unit = {
    System.clearProperty("CLUSTER_SEEDS")
  }

  test("when publicNetwork option NOT given, should bind to 127.0.0.1 with auth DISABLED | CSW-96, CSW-89") {
    val clusterPort = SocketUtils.getFreePort
    System.clearProperty("CLUSTER_SEEDS")
    System.setProperty("CLUSTER_SEEDS", s"$clusterSeedIp:$clusterPort")
    val `127.0.0.1` = "127.0.0.1"

    val (binding, wiring) = Main.start(Array("--clusterPort", s"$clusterPort")).get

    binding.localAddress.getAddress.getHostAddress shouldBe `127.0.0.1`
    SocketUtils.isAddressInUse(`127.0.0.1`, httpPort) shouldBe true

    // connect to location service running on 127.0.0.1, can access protected resource as auth is disabled
    val locationServiceClient: LocationService = HttpLocationServiceFactory.makeLocalClient(wiring.actorSystem)
    locationServiceClient.unregisterAll().futureValue

    binding.terminate(2.seconds).futureValue
    wiring.actorRuntime.shutdown().futureValue
  }

  test("when publicNetwork option is given, should bind to Public Network IP with auth ENABLED | CSW-96, CSW-89") {
    val clusterPort = SocketUtils.getFreePort
    System.clearProperty("CLUSTER_SEEDS")
    System.setProperty("CLUSTER_SEEDS", s"$clusterSeedIp:$clusterPort")
    val hostname = Networks(NetworkType.Public.envKey).hostname

    val (binding, wiring) = Main.start(Array("--clusterPort", s"$clusterPort", "--publicNetwork")).get
    //This is to make sure, it does not give AASResolution Failed error
    //This register is done via httpService impl bypassing HTTP Layer, hence dont require auth, do not confuse it with
    //calls from locationServiceClient like register, unregisterAll which will require auth, if auth is enabled
    wiring.locationService.register(HttpRegistration(AASConnection.value, SocketUtils.getFreePort, "auth")).futureValue

    binding.localAddress.getAddress.getHostAddress shouldBe hostname
    SocketUtils.isAddressInUse(hostname, httpPort) shouldBe true

    // connect to location service running on hostname, cannot access protected resource as auth is enabled
    val locationServiceClient: LocationService = HttpLocationServiceFactory.make(hostname)(wiring.actorSystem)
    val exception                              = intercept[Exception](locationServiceClient.unregisterAll().futureValue)
    exception.getCause shouldBe a[HttpError]
    exception.getCause.asInstanceOf[HttpError].statusCode shouldBe 401

    binding.terminate(2.seconds).futureValue
    wiring.actorRuntime.shutdown().futureValue
  }
}
