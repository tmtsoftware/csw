package csw.services.alarm.cli.args
import csw.services.BuildInfo
import scopt.OptionParser

class ArgsParser(name: String) {
  val parser: OptionParser[Options] = new scopt.OptionParser[Options](name) with Arguments {
    head(name, BuildInfo.version)

    cmd("init")
      .action((_, args) ⇒ args.copy(cmd = "init"))
      .text("initialize the alarm store")
      .children(filePath, localConfig, reset)

    cmd("update")
      .action((_, args) ⇒ args.copy(cmd = "update"))
      .text("set severity of an alarm")
      .children(subsystem, component, alarmName, severity)

    cmd("acknowledge")
      .action((_, args) ⇒ args.copy(cmd = "acknowledge"))
      .text("acknowledge an alarm")
      .children(subsystem, component, alarmName)

    cmd("activate")
      .action((_, args) ⇒ args.copy(cmd = "activate"))
      .text("activate an alarm")
      .children(subsystem, component, alarmName)

    cmd("deactivate")
      .action((_, args) ⇒ args.copy(cmd = "deactivate"))
      .text("deactivate an alarm")
      .children(subsystem, component, alarmName)
  }

  def parse(args: Seq[String]): Option[Options] = parser.parse(args, Options())
}
