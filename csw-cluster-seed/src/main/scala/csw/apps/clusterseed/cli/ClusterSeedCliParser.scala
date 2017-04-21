package csw.apps.clusterseed.cli

class ClusterSeedCliParser {
  val parser = new scopt.OptionParser[ClusterSeedCliOptions]("scopt") {
    head("csw-cluster-seed", System.getProperty("CSW_VERSION"))

    opt[Int]("clusterPort").required() action { (x, c) =>
      c.copy(clusterPort = x)
    } text "Port at which this cluster seed will run"

    opt[String]("clusterSeeds").required() action { (x, c) =>
      c.copy(clusterSeeds = x)
    } text "Addresses of all seed nodes including this node separated by commas"

    help("help")

    version("version")
  }

  def parse(args: Seq[String]): Option[ClusterSeedCliOptions] = parser.parse(args, ClusterSeedCliOptions())
}
