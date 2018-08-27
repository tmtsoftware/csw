package csw.services.alarm.cli.args
import csw.services.BuildInfo
import scopt.OptionParser

class ArgsParser(name: String) {
  val parser: OptionParser[Options] = new scopt.OptionParser[Options](name) with Arguments {
    head(name, BuildInfo.version)

    private def requiredAlarmKey = List(subsystem.required(), component.required(), alarmName.required())
    private def optionalAlarmKey = List(subsystem, component, alarmName)

    cmd("init")
      .action((_, args) ⇒ args.copy(cmd = "init"))
      .text("initialize the alarm store")
      .children(filePath, localConfig, reset)

    cmd("severity")
      .action((_, args) ⇒ args.copy(cmd = "severity"))
      .children(
        cmd("get")
          .action((_, args) ⇒ args.copy(subCmd = "get"))
          .children(optionalAlarmKey: _*)
          .text("get severity of a subsystem/component/alarm"),
        cmd("set")
          .action((_, args) ⇒ args.copy(subCmd = "set"))
          .children(requiredAlarmKey :+ severity :+ refresh: _*)
          .text("set severity of an alarm"),
        cmd("subscribe")
          .action((_, args) ⇒ args.copy(subCmd = "subscribe"))
          .children(optionalAlarmKey: _*)
          .text("subscribe to severity of a subsystem/component/alarm")
      )

    cmd("health")
      .action((_, args) ⇒ args.copy(cmd = "health"))
      .children(
        cmd("get")
          .action((_, args) ⇒ args.copy(subCmd = "get"))
          .children(optionalAlarmKey: _*)
          .text("get health of a subsystem/component/alarm"),
        cmd("subscribe")
          .action((_, args) ⇒ args.copy(subCmd = "subscribe"))
          .children(optionalAlarmKey: _*)
          .text("subscribe to health of a subsystem/component/alarm")
      )

    cmd("acknowledge")
      .action((_, args) ⇒ args.copy(cmd = "acknowledge"))
      .text("acknowledge an alarm")
      .children(requiredAlarmKey: _*)

    cmd("unacknowledge")
      .action((_, args) ⇒ args.copy(cmd = "unacknowledge"))
      .text("unacknowledge an alarm")
      .children(requiredAlarmKey: _*)

    cmd("activate")
      .action((_, args) ⇒ args.copy(cmd = "activate"))
      .text("activate an alarm")
      .children(requiredAlarmKey: _*)

    cmd("deactivate")
      .action((_, args) ⇒ args.copy(cmd = "deactivate"))
      .text("deactivate an alarm")
      .children(requiredAlarmKey: _*)

    cmd("shelve")
      .action((_, args) ⇒ args.copy(cmd = "shelve"))
      .text("shelve an alarm")
      .children(requiredAlarmKey: _*)

    cmd("unshelve")
      .action((_, args) ⇒ args.copy(cmd = "unshelve"))
      .text("unshelve an alarm")
      .children(requiredAlarmKey: _*)

    cmd("reset")
      .action((_, args) ⇒ args.copy(cmd = "reset"))
      .text("reset latched severity of an alarm")
      .children(requiredAlarmKey: _*)

    cmd("list")
      .action((_, args) ⇒ args.copy(cmd = "list"))
      .text("list alarms")
      .children(optionalAlarmKey: _*)

    cmd("status")
      .action((_, args) ⇒ args.copy(cmd = "status"))
      .text("get current status of the alarm")
      .children(requiredAlarmKey: _*)

    help("help")

    version("version")

    checkConfig { c =>
      val commandsAllowingPartialKey = List("list", "severity", "health")
      val commandsHavingSubCommands  = List("severity", "health")

      if (c.cmd.isEmpty)
        failure("""
                  |Please specify one of the following command with their corresponding options:
                  |  1> init
                  |  2> severity
                  |  3> acknowledge
                  |  4> unacknowledge
                  |  5> activate
                  |  6> deactivate
                  |  7> shelve
                  |  8> unshelve
                  |  9> reset
                  |  10> list
                  |  11> status
                  |  12> health
                """.stripMargin)
      else if (commandsHavingSubCommands.contains(c.cmd) && c.subCmd.isEmpty)
        failure("Please specify an appropriate sub-command")
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
