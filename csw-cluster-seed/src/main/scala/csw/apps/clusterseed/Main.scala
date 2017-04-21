package csw.apps.clusterseed

import csw.apps.clusterseed.cli.{ArgsParser, Options}
import csw.services.location.commons.CswCluster

class Main {

  def start(args: Array[String]): Unit = {

    val clusterSeedCliParser = new ArgsParser

    clusterSeedCliParser.parse(args).foreach {
      case Options(port, clusterSeeds) =>
        sys.props("clusterSeeds") = clusterSeeds
        sys.props("clusterPort") = port.toString

        CswCluster.make()
    }
  }
}

object Main extends App {
  new Main().start(args)
}
