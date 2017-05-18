package csw.apps.clusterseed.cli

import csw.services.BuildInfo

/**
 * Parses the command line options using `scopt` library.
 */
class ArgsParser {
  val parser = new scopt.OptionParser[Options]("scopt") {
    head(BuildInfo.name, BuildInfo.version)

    opt[Int]("clusterPort").required() action { (x, c) =>
      c.copy(clusterPort = x)
    } text "Port at which this cluster seed will run"

    help("help")

    version("version")
  }

  def parse(args: Seq[String]): Option[Options] = parser.parse(args, Options())
}
