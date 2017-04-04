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
    val clusterSettings: ClusterSettings = ClusterSettings().withInterface("en0").onPort(8888).joinSeeds("10.10.10.10")
    val config = clusterSettings.config

    config.getString("akka.remote.netty.tcp.hostname") shouldBe new Networks().hostname()
    config.getInt("akka.remote.netty.tcp.port") shouldBe 8888
    config.getStringList("akka.cluster.seed-nodes").asScala shouldBe List("10.10.10.10").map{ hostname =>
      s"akka.tcp://${clusterSettings.clusterName}@$hostname"}
  }

  test("cluster settings with multiple seeds"){
    val clusterSettings: ClusterSettings = ClusterSettings().joinSeeds("10.10.10.10", "10.10.10.10")
    val config = clusterSettings.config

    config.getString("akka.remote.netty.tcp.hostname") shouldBe new Networks().hostname()
    config.getInt("akka.remote.netty.tcp.port") shouldBe 0
    config.getStringList("akka.cluster.seed-nodes").asScala shouldBe List("10.10.10.10", "10.10.10.10").map{ hostname =>
      s"akka.tcp://${clusterSettings.clusterName}@$hostname"}
  }
}
