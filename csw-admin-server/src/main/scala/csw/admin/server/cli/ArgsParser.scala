package csw.admin.server.cli

import csw.services.BuildInfo
import scopt.OptionParser

/**
 * Parses the command line options using `scopt` library.
 */
class ArgsParser(name: String) {

  private val parser: OptionParser[Options] = new scopt.OptionParser[Options](name) {
    head(name, BuildInfo.version)

    opt[Int]("port") action { (x, c) =>
      c.copy(adminPort = Some(x))
    } text "Optional: Port at which the http admin log server will start. Default is 7878"

    opt[String]("locationHost") action { (x, c) =>
      c.copy(locationHost = x)
    } text "Optional: host address of machine where location server is running. Default is localhost"

    help("help")

    version("version")
  }

  def parse(args: Seq[String]): Option[Options] = parser.parse(args, Options())
}
