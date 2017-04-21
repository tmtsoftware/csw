package csw.apps.clusterseed.cli

import csw.services.BuildInfo

class ArgsParser {
  val parser = new scopt.OptionParser[Options]("scopt") {
    head(BuildInfo.name, BuildInfo.version)

    opt[Int]("clusterPort").required() action { (x, c) =>
      c.copy(clusterPort = x)
    } text "Port at which this cluster seed will run"

    opt[String]("clusterSeeds").required() action { (x, c) =>
      c.copy(clusterSeeds = x)
    } text "Addresses of all seed nodes including this node separated by commas"

    help("help")

    version("version")
  }

  def parse(args: Seq[String]): Option[Options] = parser.parse(args, Options())
}
