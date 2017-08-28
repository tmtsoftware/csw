package csw.apps.containercmd.cli

import java.nio.file.Paths

import csw.services.BuildInfo

/**
 * Parses the command line options using `scopt` library.
 */
class ArgsParser() {
  val parser = new scopt.OptionParser[Options]("scopt") {
    head(BuildInfo.name, BuildInfo.version)

    opt[Unit]("standalone") action { (_, c) =>
      c.copy(standalone = true)
    } text "run component in standalone mode, without a container"

    opt[String]("local") action { (x, c) =>
      c.copy(local = true)
    } text s"run using the file on local file system without fetching file from config service"

    arg[String]("<file>") maxOccurs 1 action { (x, c) =>
      c.copy(inputFilePath = Some(Paths.get(x)))
    } text "config file path"

    help("help")
    version("version")
  }

  def parse(args: Seq[String]): Option[Options] = parser.parse(args, Options())
}
