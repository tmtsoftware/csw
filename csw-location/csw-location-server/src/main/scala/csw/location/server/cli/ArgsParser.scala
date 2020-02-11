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

    opt[Unit]("public")
      .action((_, c) => c.copy(public = true))
      .text(
        "binds location service to all hosts making it available to other machines in the " +
          "network. This will also enable auth by default."
      )

    help("help")

    version("version")
  }

  def parse(args: Seq[String]): Option[Options] = parser.parse(args, Options())
}
