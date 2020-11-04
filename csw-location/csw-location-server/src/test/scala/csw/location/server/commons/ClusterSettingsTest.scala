package csw.location.server.commons

import com.typesafe.config.ConfigException
import csw.location.api.commons.Constants
import csw.location.api.models.NetworkType
import csw.network.utils.Networks
import io.netty.util.internal.logging.{InternalLoggerFactory, Slf4JLoggerFactory}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

class ClusterSettingsTest extends AnyFunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  // Fix to avoid 'java.util.concurrent.RejectedExecutionException: Worker has already been shutdown'
  InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE)

  override protected def afterAll(): Unit = {
    System.clearProperty("CLUSTER_SEEDS")
    // CSW-97
    System.clearProperty(NetworkType.Private.envKey)
    System.clearProperty(NetworkType.Public.envKey)
  }

  test("exception is thrown when settings are not found for a given cluster name") {
    val settings: ClusterSettings = ClusterSettings("undefined-settings-in-conf")
    a[ConfigException.Missing] shouldBe thrownBy(settings.config)
  }

  test("default cluster settings are used when no custom parameters are supplied") {
    val clusterSettings = ClusterSettings()
    val config          = clusterSettings.config

    clusterSettings.clusterName shouldBe Constants.ClusterName
    config.getString("akka.remote.artery.canonical.hostname") shouldBe Networks().hostname
    config.getInt("akka.remote.artery.canonical.port") shouldBe 0
    config.getList("akka.cluster.seed-nodes").size shouldBe 0
  }

  test("cluster settings with custom parameters are used") {
    val port = 8888
    val clusterSettings: ClusterSettings =
      ClusterSettings().withInterface("en0").onPort(port).joinSeeds("10.10.10.10, 10.10.10.11")

    clusterSettings.interfaceName shouldBe Some("en0")
    clusterSettings.port shouldBe port
    clusterSettings.seedNodes shouldBe List("10.10.10.10", "10.10.10.11").map { hostname =>
      s"akka://${clusterSettings.clusterName}@$hostname"
    }
  }

  test("cluster settings with join Local") {
    val port     = 9010
    val portList = List(9000, 9001, 9002, 9003)
    val hostname = Networks().hostname
    val clusterSettings: ClusterSettings =
      ClusterSettings().onPort(port).joinLocal(portList(0), portList(1), portList(2), portList(3))

    clusterSettings.port shouldBe port
    clusterSettings.seedNodes shouldBe portList.map { port => s"akka://${clusterSettings.clusterName}@$hostname:$port" }
  }

  test("cluster settings with custom values | CSW-97") {
    val port   = 9001
    val ipList = List("10.10.10.10", "10.10.10.11", "10.10.10.12")
    val values = Map(
      NetworkType.Private.envKey -> "en0",
      NetworkType.Public.envKey  -> "en1",
      "CLUSTER_SEEDS"            -> ipList.mkString(", "),
      "CLUSTER_PORT"             -> "9000"
    )
    val clusterSettings: ClusterSettings = ClusterSettings(values = values).onPort(port)

    clusterSettings.interfaceName shouldBe Some("en0")
    clusterSettings.publicInterfaceName shouldBe Some("en1")
    clusterSettings.port shouldBe port
    clusterSettings.seedNodes shouldBe ipList.map { hostname => s"akka://${clusterSettings.clusterName}@$hostname" }
  }

  test("cluster settings with system properties | CSW-97") {
    val systemPort      = 9002
    val systemSeeds     = "10.10.10.12, 10.10.10.13"
    val systemInterface = "eth0"
    val publicInterface = "eth1"

    System.setProperty("CLUSTER_SEEDS", systemSeeds.toString)
    System.setProperty(NetworkType.Private.envKey, systemInterface)
    System.setProperty(NetworkType.Public.envKey, publicInterface)

    val clusterSettings = ClusterSettings().onPort(systemPort)

    clusterSettings.port shouldBe systemPort
    clusterSettings.interfaceName shouldBe Some(systemInterface)
    clusterSettings.publicInterfaceName shouldBe Some(publicInterface)
    clusterSettings.seedNodes shouldBe List("10.10.10.12", "10.10.10.13").map { hostname =>
      s"akka://${clusterSettings.clusterName}@$hostname"
    }
  }
}
