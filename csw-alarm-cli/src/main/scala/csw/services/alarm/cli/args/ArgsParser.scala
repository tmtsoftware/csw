package csw.services.alarm.cli.args
import java.nio.file.Paths

import csw.services.BuildInfo
import csw.services.alarm.api.models.AlarmSeverity
import scopt.OptionParser

class ArgsParser(name: String) {
  val parser: OptionParser[CommandLineArgs] = new scopt.OptionParser[CommandLineArgs](name) {
    head(name, BuildInfo.version)

    cmd("init")
      .action((_, args) ⇒ args.copy(cmd = "init"))
      .text("initialize the alarm store")
      .children(
        arg[String]("<filePath>")
          .required()
          .action((filePath, args) ⇒ args.copy(filePath = Some(Paths.get(filePath))))
          .text("path to the file to load the alarm data"),
        opt[Unit]("local")
          .action((_, args) ⇒ args.copy(isLocal = true))
          .text("optional pick file path from local disk. By default it will load from configuration service"),
        opt[Unit]("reset")
          .action((_, args) ⇒ args.copy(reset = true))
          .text("optional reset the alarm store data")
      )

    cmd("update")
      .action((_, args) ⇒ args.copy(cmd = "update"))
      .text("set severity of an alarm")
      .children(
        opt[String]("subsystem")
          .required()
          .action((subsystem, args) ⇒ args.copy(subsystem = subsystem))
          .text("subsystem of an alarm e.g. NFIRAOS"),
        opt[String]("component")
          .required()
          .action((component, args) ⇒ args.copy(component = component))
          .text("component of an alarm e.g. trombone"),
        opt[String]("name")
          .required()
          .action((name, args) ⇒ args.copy(name = name))
          .text("name of an alarm e.g. tromboneAxisHighLimitAlarm"),
        opt[String]("severity")
          .required()
          .action((severity, args) ⇒ args.copy(severity = AlarmSeverity.withNameInsensitive(severity)))
          .text("severity to set for an alarm e.g Okay, Warning, Major, etc.")
      )
  }

  def parse(args: Seq[String]): Option[CommandLineArgs] = parser.parse(args, CommandLineArgs())
}
