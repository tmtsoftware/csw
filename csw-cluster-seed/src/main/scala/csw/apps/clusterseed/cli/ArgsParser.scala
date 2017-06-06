package csw.apps.clusterseed.cli

import csw.services.BuildInfo

/**
 * Parses the command line options using `scopt` library.
 */
class ArgsParser {
  val parser = new scopt.OptionParser[Options]("scopt") {
    head(BuildInfo.name, BuildInfo.version)

    opt[Int]("clusterPort") required () action { (x, c) =>
      c.copy(clusterPort = x)
    } text "Port at which this cluster seed will run"

    opt[Int]("adminPort") action { (x, c) =>
      c.copy(adminPort = Some(x))
    } text "Optional: Port at which the http admin log server will start. Default is 7878"

    help("help")

    version("version")
  }

  def parse(args: Seq[String]): Option[Options] = parser.parse(args, Options())
}
