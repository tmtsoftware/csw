package csw.location.server

import csw.location.api.models.NetworkType
import csw.network.utils.{Networks, SocketUtils}
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers

class MainTest extends AnyFunSuiteLike with Matchers {
  private val clusterSeedIp = Networks().hostname
  private val httpPort      = 7654

  test("Bind location server to 127.0.0.1 without publicNetwork option | CSW-96, CSW-89") {
    val clusterPort = SocketUtils.getFreePort
    System.clearProperty("CLUSTER_SEEDS")
    System.setProperty("CLUSTER_SEEDS", s"$clusterSeedIp:$clusterPort")
    val localhost = "127.0.0.1"

    val (binding, _) = Main.start(Array("--clusterPort", s"$clusterPort")).get

    binding.localAddress.getAddress.getHostAddress shouldBe localhost
    SocketUtils.isAddressInUse(localhost, httpPort) shouldBe true
  }

  test("Bind location server to Public Network IP with publicNetwork option | CSW-96, CSW-89") {
    val clusterPort = SocketUtils.getFreePort
    System.clearProperty("CLUSTER_SEEDS")
    System.setProperty("CLUSTER_SEEDS", s"$clusterSeedIp:$clusterPort")
    val hostname = Networks(NetworkType.Public.envKey).hostname

    val (binding, _) = Main.start(Array("--clusterPort", s"$clusterPort", "--publicNetwork")).get

    binding.localAddress.getAddress.getHostAddress shouldBe hostname
    SocketUtils.isAddressInUse(hostname, httpPort) shouldBe true
  }
}
