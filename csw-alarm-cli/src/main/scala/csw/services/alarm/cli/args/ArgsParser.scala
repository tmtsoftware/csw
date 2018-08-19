package csw.services.alarm.cli.args
import csw.services.BuildInfo
import scopt.OptionParser

class ArgsParser(name: String) {
  val parser: OptionParser[Options] = new scopt.OptionParser[Options](name) with Arguments {
    head(name, BuildInfo.version)

    private def alarmKey  = List(subsystem.required(), component.required(), alarmName.required())
    private val get       = cmd("get").action((_, args) ⇒ args.copy(subCmd = "get")).children(alarmKey: _*)
    private val set       = cmd("set").action((_, args) ⇒ args.copy(subCmd = "set")).children(alarmKey :+ severity: _*)
    private val subscribe = cmd("subscribe").action((_, args) ⇒ args.copy(subCmd = "subscribe")).children(alarmKey: _*)

    cmd("init")
      .action((_, args) ⇒ args.copy(cmd = "init"))
      .text("initialize the alarm store")
      .children(filePath, localConfig, reset)

    cmd("severity")
      .action((_, args) ⇒ args.copy(cmd = "severity"))
      .text("get/set/subscribe severity of an alarm")
      .children(get, set, subscribe)

    cmd("acknowledge")
      .action((_, args) ⇒ args.copy(cmd = "acknowledge"))
      .text("acknowledge an alarm")
      .children(alarmKey: _*)

    cmd("unacknowledge")
      .action((_, args) ⇒ args.copy(cmd = "unacknowledge"))
      .text("unacknowledge an alarm")
      .children(alarmKey: _*)

    cmd("activate")
      .action((_, args) ⇒ args.copy(cmd = "activate"))
      .text("activate an alarm")
      .children(alarmKey: _*)

    cmd("deactivate")
      .action((_, args) ⇒ args.copy(cmd = "deactivate"))
      .text("deactivate an alarm")
      .children(alarmKey: _*)

    cmd("shelve")
      .action((_, args) ⇒ args.copy(cmd = "shelve"))
      .text("shelve an alarm")
      .children(alarmKey: _*)

    cmd("unshelve")
      .action((_, args) ⇒ args.copy(cmd = "unshelve"))
      .text("unshelve an alarm")
      .children(alarmKey: _*)

    cmd("reset")
      .action((_, args) ⇒ args.copy(cmd = "reset"))
      .text("reset latched severity of an alarm")
      .children(alarmKey: _*)

    cmd("list")
      .action((_, args) ⇒ args.copy(cmd = "list"))
      .text("list alarms")
      .children(subsystem, component, alarmName)

    cmd("status")
      .action((_, args) ⇒ args.copy(cmd = "status"))
      .text("get current status of the alarm")
      .children(alarmKey: _*)

    help("help")

    version("version")

    checkConfig { c =>
      val commandsAllowingPartialKey = List("list")
      if (c.cmd.isEmpty)
        failure("""
                  |Please specify one of the following command with their corresponding options:
                  |  1> init
                  |  2> update
                  |  3> acknowledge
                  |  4> unacknowledge
                  |  5> activate
                  |  6> deactivate
                  |  7> shelve
                  |  8> unshelve
                  |  9> reset
                  |  10> list
                  |  11> status
                """.stripMargin)
      else if (commandsAllowingPartialKey.contains(c.cmd)) validateKey(c)
      else success
    }

    private def validateKey(c: Options) = (c.maybeSubsystem, c.maybeComponent, c.maybeAlarmName) match {
      case (None, None, Some(_)) | (Some(_), None, Some(_)) ⇒ failure("Please specify subsystem and component of the alarm.")
      case (None, Some(_), _)                               ⇒ failure("Please specify subsystem of the component.")
      case _                                                ⇒ success
    }
  }

  def parse(args: Seq[String]): Option[Options] = parser.parse(args, Options())
}
