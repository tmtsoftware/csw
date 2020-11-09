package csw.location.server.cli

import csw.location.server.BuildInfo
import scopt.OptionParser

/**
 * Parses the command line options using `scopt` library.
 */
class ArgsParser(name: String) {

  private val parser: OptionParser[Options] = new scopt.OptionParser[Options](name) {
    head(name, BuildInfo.version)

    opt[Int]("clusterPort") action { (x, c) =>
      c.copy(clusterPort = Some(x))
    } text "Optional: Port at which this cluster will run. Default is 3552"

    opt[Unit]("outsideNetwork")
      .action((_, c) => c.copy(outsideNetwork = true))
      .text(
        "Optional: Binds http location service to outside network IP making it available to machines in the " +
          "outside network. This will also enable auth by default."
      )

    help("help")

    version("version")
  }

  def parse(args: Seq[String]): Option[Options] = parser.parse(args, Options())
}
