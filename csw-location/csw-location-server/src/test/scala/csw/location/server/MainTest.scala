package csw.location.server

import csw.location.api.models.NetworkType
import csw.network.utils.{Networks, SocketUtils}
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

  test("Bind location server to 127.0.0.1 without publicNetwork option | CSW-96, CSW-89") {
    val clusterPort = SocketUtils.getFreePort
    System.clearProperty("CLUSTER_SEEDS")
    System.setProperty("CLUSTER_SEEDS", s"$clusterSeedIp:$clusterPort")
    val `127.0.0.1` = "127.0.0.1"

    val (binding, wiring) = Main.start(Array("--clusterPort", s"$clusterPort")).get

    binding.localAddress.getAddress.getHostAddress shouldBe `127.0.0.1`
    SocketUtils.isAddressInUse(`127.0.0.1`, httpPort) shouldBe true
    binding.terminate(2.seconds).futureValue
    wiring.actorRuntime.shutdown().futureValue
  }

  test("Bind location server to Public Network IP with publicNetwork option | CSW-96, CSW-89") {
    val clusterPort = SocketUtils.getFreePort
    System.clearProperty("CLUSTER_SEEDS")
    System.setProperty("CLUSTER_SEEDS", s"$clusterSeedIp:$clusterPort")
    val hostname = Networks(NetworkType.Public.envKey).hostname

    val (binding, wiring) = Main.start(Array("--clusterPort", s"$clusterPort", "--publicNetwork")).get

    binding.localAddress.getAddress.getHostAddress shouldBe hostname
    SocketUtils.isAddressInUse(hostname, httpPort) shouldBe true
    binding.terminate(2.seconds).futureValue
    wiring.actorRuntime.shutdown().futureValue
  }
}
