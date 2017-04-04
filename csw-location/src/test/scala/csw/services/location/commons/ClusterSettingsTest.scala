package csw.services.location.commons

import com.typesafe.config.ConfigException
import csw.services.location.internal.Networks
import org.scalatest.{FunSuite, Matchers}

import scala.collection.JavaConverters._

class ClusterSettingsTest
  extends FunSuite
    with Matchers {

  test("exception is thrown when settings are not found for a given cluster name") {
    val settings: ClusterSettings = ClusterSettings("undefined-settings-in-conf")
    intercept[ConfigException.Missing] {
      settings.config
    }
  }

  test("default cluster settings are used when no custom parameters are supplied") {
    val clusterSettings = ClusterSettings()
    val config = clusterSettings.config

    clusterSettings.clusterName shouldBe Constants.ClusterName
    config.getString("akka.remote.netty.tcp.hostname") shouldBe new Networks().hostname()
    config.getInt("akka.remote.netty.tcp.port") shouldBe 0
    config.getList("akka.cluster.seed-nodes").size shouldBe 0
  }

  test("cluster settings with custom parameters are used"){
    val clusterSettings: ClusterSettings = ClusterSettings().withInterface("en0").onPort(8888).joinSeeds("10.10.10.10", "10.10.10.11")
    val config = clusterSettings.config

    config.getString("akka.remote.netty.tcp.hostname") shouldBe new Networks("en0").hostname()
    config.getInt("akka.remote.netty.tcp.port") shouldBe 8888
    config.getStringList("akka.cluster.seed-nodes").asScala shouldBe List("10.10.10.10", "10.10.10.11").map{ hostname =>
      s"akka.tcp://${clusterSettings.clusterName}@$hostname"}
  }

  test("cluster settings with join Local"){
    val portList = List(9000, 9001, 9002, 9003)
    val hostname = new Networks().hostname
    val clusterSettings: ClusterSettings = ClusterSettings().onPort(9010).joinLocal(portList(0), portList(1), portList(2), portList(3))
    val config = clusterSettings.config

    config.getInt("akka.remote.netty.tcp.port") shouldBe 9010
    config.getStringList("akka.cluster.seed-nodes").asScala shouldBe portList.map{ port =>
      s"akka.tcp://${clusterSettings.clusterName}@$hostname:$port"}
  }

  test("cluster settings with custom values") {
    val ipList = List("10.10.10.10", "10.10.10.11", "10.10.10.12")
    val values = Map("interfaceName" -> "en0",
    "clusterSeeds" -> ipList.mkString(", "),
    "clusterPort" -> "9000")
    val clusterSettings: ClusterSettings = ClusterSettings(values = values).onPort(9001)
    val config = clusterSettings.config

    config.getString("akka.remote.netty.tcp.hostname") shouldBe new Networks("en0").hostname()
    config.getInt("akka.remote.netty.tcp.port") shouldBe 9001
    config.getStringList("akka.cluster.seed-nodes").asScala shouldBe ipList.map{ hostname =>
      s"akka.tcp://${clusterSettings.clusterName}@$hostname"}
  }
}
