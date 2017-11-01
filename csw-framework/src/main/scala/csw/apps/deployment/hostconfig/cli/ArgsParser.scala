package csw.apps.deployment.hostconfig.cli

import java.nio.file.Paths

import csw.services.BuildInfo
import scopt.OptionParser

/**
 * Parses the command line options using `scopt` library.
 */
class ArgsParser() {
  val parser: OptionParser[Options] = new scopt.OptionParser[Options]("scopt") {
    head(BuildInfo.name, BuildInfo.version)

    opt[Unit]("local") action { (_, c) =>
      c.copy(local = true)
    } text "if provided, get the host configuration file from local machine located at hostConfigPath, else fetch it from config service"

    arg[String]("<file>") required () action { (x, c) =>
      c.copy(hostConfigPath = Some(Paths.get(x)))
    } text "host configuration file path"

    help("help")
    version("version")
  }

  def parse(args: Seq[String]): Option[Options] = parser.parse(args, Options())
}
