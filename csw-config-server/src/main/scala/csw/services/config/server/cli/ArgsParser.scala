package csw.services.config.server.cli

import csw.services.BuildInfo

/**
 * Parses the command line options using `scopt` library.
 */
class ArgsParser(name: String) {

  private val parser = new scopt.OptionParser[Options](name) {
    head(name, BuildInfo.version)

    opt[Unit]("initRepo") action { (x, c) =>
      c.copy(initRepo = true)
    } text "optional, if specified the repository will be initialized, if it does not exist"

    opt[Int]("port") action { (x, c) =>
      c.copy(port = Some(x))
    } text "http port at which service will be run, if not specified value from config file will be used"

    help("help")

    version("version")
  }

  def parse(args: Seq[String]): Option[Options] = parser.parse(args, Options())
}
