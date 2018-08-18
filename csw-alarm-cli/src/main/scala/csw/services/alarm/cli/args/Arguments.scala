package csw.services.alarm.cli.args

import java.nio.file.Paths

import csw.messages.params.models.Subsystem
import csw.services.alarm.api.models.AlarmSeverity
import scopt.{OptionDef, OptionParser}

trait Arguments { self: OptionParser[Options] =>
  def filePath: OptionDef[String, Options] =
    arg[String]("<filePath>")
      .required()
      .action((filePath, args) ⇒ args.copy(filePath = Some(Paths.get(filePath))))
      .text("path to the file to load the alarm data")

  def localConfig: OptionDef[Unit, Options] =
    opt[Unit]("local")
      .action((_, args) ⇒ args.copy(isLocal = true))
      .text("optional pick file path from local disk. By default it will load from configuration service")

  def reset: OptionDef[Unit, Options] =
    opt[Unit]("reset")
      .action((_, args) ⇒ args.copy(reset = true))
      .text("optional reset the alarm store data")

  def subsystem: OptionDef[String, Options] =
    opt[String]("subsystem")
      .action((subsystemName, args) ⇒ args.copy(maybeSubsystem = Some(Subsystem.withNameInsensitive(subsystemName))))
      .text("subsystem of an alarm e.g. NFIRAOS")

  def component: OptionDef[String, Options] =
    opt[String]("component")
      .action((component, args) ⇒ args.copy(maybeComponent = Some(component)))
      .text("component of an alarm e.g. trombone")

  def alarmName: OptionDef[String, Options] =
    opt[String]("name")
      .action((name, args) ⇒ args.copy(maybeAlarmName = Some(name)))
      .text("name of an alarm e.g. tromboneAxisHighLimitAlarm")

  def severity: OptionDef[String, Options] =
    opt[String]("severity")
      .action(
        (severity, args) ⇒ args.copy(severity = Some(AlarmSeverity.withNameInsensitive(severity)))
      )
      .text("severity to set for an alarm e.g Okay, Warning, Major, etc.")
}
