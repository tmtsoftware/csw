package csw.apps.deployment.containercmd.cli

import java.nio.file.Paths

import csw.services.BuildInfo
import scopt.OptionParser

/**
 * Parses the command line options using `scopt` library.
 */
class ArgsParser() {
  val parser: OptionParser[Options] = new scopt.OptionParser[Options]("scopt") {
    head(BuildInfo.name, BuildInfo.version)

    opt[Unit]("standalone") action { (_, c) =>
      c.copy(standalone = true)
    } text "run component in standalone mode, without a container"

    opt[Unit]("local") action { (_, c) =>
      c.copy(local = true)
    } text "run using the file on local file system without fetching file from config service"

    arg[String]("<file>") maxOccurs 1 minOccurs 0 action { (x, c) =>
      c.copy(inputFilePath = Some(Paths.get(x)))
    } text "config file path"

    help("help")
    version("version")
  }

  def parse(args: Seq[String]): Option[Options] = parser.parse(args, Options())
}
