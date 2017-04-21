package csw.apps.clusterseed

import csw.apps.clusterseed.cli.{ClusterSeedCliOptions, ClusterSeedCliParser}
import csw.services.location.commons.CswCluster

class ClusterSeedCliApp {

  def start(args: Array[String]): Unit = {

    val clusterSeedCliParser = new ClusterSeedCliParser

    clusterSeedCliParser.parse(args).foreach {
      case ClusterSeedCliOptions(port, clusterSeeds) =>
        sys.props("clusterSeeds") = clusterSeeds
        sys.props("clusterPort") = port.toString

        CswCluster.make()
    }
  }
}

object ClusterSeedCliApp extends App {
  new ClusterSeedCliApp().start(args)
}
