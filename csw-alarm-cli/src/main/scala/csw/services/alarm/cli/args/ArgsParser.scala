package csw.services.alarm.cli.args
import java.nio.file.Paths

import scopt.OptionParser
import csw.services.BuildInfo

class ArgsParser(name: String) {
  val parser: OptionParser[CommandLineArgs] = new scopt.OptionParser[CommandLineArgs](name) {
    head(name, BuildInfo.version)

    cmd("init")
      .action((_, options) ⇒ options.copy(cmd = "init"))
      .text("initialize the alarm store")
      .children {
        arg[String]("<filePath>")
          .required()
          .action((filePath, options) ⇒ options.copy(filePath = Some(Paths.get(filePath))))
          .text("path to the file to load the alarm data")
        opt[Unit]("local")
          .action((_, options) ⇒ options.copy(isLocal = true))
          .text("optional pick file path from local disk. By default it will load from configuration service")
        opt[Unit]("reset")
          .action((_, options) ⇒ options.copy(reset = true))
          .text("optional reset the alarm store data")
      }
  }

  def parse(args: Seq[String]): Option[CommandLineArgs] = parser.parse(args, CommandLineArgs())
}
