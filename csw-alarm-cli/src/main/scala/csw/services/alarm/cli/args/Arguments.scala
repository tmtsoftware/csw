package csw.services.alarm.cli.args

import java.nio.file.Paths

import csw.services.alarm.api.models.AlarmSeverity
import csw.services.alarm.api.models.Key.AlarmKey
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

  def alarmKey: OptionDef[String, Options] =
    opt[String]("alarmKey")
      .required()
      .action((key, args) ⇒ {
        args.copy(alarmKey = AlarmKey(key))
      })
      .text("alarm key e.g. nfiraos.trombone.tromboneaxishighlimitalarm")

  def subsystem: OptionDef[String, Options] =
    opt[String]("subsystem")
      .required()
      .action((subsystem, args) ⇒ args.copy(subsystem = subsystem))
      .text("subsystem of an alarm e.g. NFIRAOS")

  def component: OptionDef[String, Options] =
    opt[String]("component")
      .required()
      .action((component, args) ⇒ args.copy(component = component))
      .text("component of an alarm e.g. trombone")

  def alarmName: OptionDef[String, Options] =
    opt[String]("name")
      .required()
      .action((name, args) ⇒ args.copy(name = name))
      .text("name of an alarm e.g. tromboneAxisHighLimitAlarm")

  def severity: OptionDef[String, Options] =
    opt[String]("severity")
      .required()
      .action((severity, args) ⇒ args.copy(severity = AlarmSeverity.withNameInsensitive(severity)))
      .text("severity to set for an alarm e.g Okay, Warning, Major, etc.")
}
