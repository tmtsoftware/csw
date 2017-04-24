package csw.apps.clusterseed

import csw.apps.clusterseed.cli.{ArgsParser, Options}
import csw.services.location.commons.{ClusterAwareSettings, CswCluster}

class Main {

  def start(args: Array[String]): Unit = {

    val clusterSeedCliParser = new ArgsParser

    clusterSeedCliParser.parse(args).foreach {
      case Options(port) =>
        val clusterSettings = ClusterAwareSettings.onPort(port)
        if (clusterSettings.seedNodes.isEmpty) {
          println(
            s"[error] clusterSeeds setting is not configured. Please do so by either setting the env variable or system property."
          )
        } else {
          clusterSettings.debug()
          CswCluster.withSettings(clusterSettings)
        }

    }
  }
}

object Main extends App {
  new Main().start(args)
}
