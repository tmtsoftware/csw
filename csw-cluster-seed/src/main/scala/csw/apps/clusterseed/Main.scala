package csw.apps.clusterseed

import csw.apps.clusterseed.cli.{ArgsParser, Options}
import csw.services.location.commons.{ClusterAwareSettings, ClusterSettings, CswCluster}

class Main(clusterSettings: ClusterSettings) {

  if (clusterSettings.seedNodes.isEmpty) {
    println(
      s"[error] clusterSeeds setting is not configured. Please do so by either setting the env variable or system property."
    )
    System.exit(1)
  }

  def start(args: Array[String]): Unit = {

    val clusterSeedCliParser = new ArgsParser

    clusterSeedCliParser.parse(args).foreach {
      case Options(port) =>
        val updatedClusterSettings = clusterSettings.onPort(port)
        updatedClusterSettings.debug()
        CswCluster.withSettings(updatedClusterSettings)
    }
  }
}

object Main extends App {
  new Main(ClusterAwareSettings).start(args)
}
