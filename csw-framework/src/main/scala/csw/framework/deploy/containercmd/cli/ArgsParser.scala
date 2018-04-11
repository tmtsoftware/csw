package csw.framework.deploy.containercmd.cli

import java.nio.file.Paths

import csw.services.BuildInfo
import scopt.OptionParser

/**
 * Parses the command line options using `scopt` library.
 */
private[containercmd] class ArgsParser(name: String) {

  val parser: OptionParser[Options] = new scopt.OptionParser[Options](name) {
    head(name, BuildInfo.version)

    opt[Unit]("standalone") action { (_, c) =>
      c.copy(standalone = true)
    } text "Optional: if provided then run component in standalone mode else run in container"

    opt[Unit]("local") action { (_, c) =>
      c.copy(local = true)
    } text "Optional: if provided then run using the file on local file system else fetch it from config service"

    arg[String]("<file>") required () action { (x, c) =>
      c.copy(inputFilePath = Some(Paths.get(x)))
    } text "specifies config file path which gets fetched from config service or local file system based on other options"

    help("help")
    version("version")
  }

  def parse(args: Seq[String]): Option[Options] = parser.parse(args, Options())
}
