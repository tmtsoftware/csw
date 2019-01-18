package csw.alarm.cli.utils

import csw.alarm.api.internal.Separators.KeySeparator
import csw.alarm.api.models.Key._
import csw.alarm.api.models._
import csw.alarm.cli.args.Options
import csw.alarm.client.internal.models.Alarm

object Formatter {

  val Newline = "\n"
  val Separator =
    "==============================================================================================================="

  def formatAlarms(alarms: List[Alarm], options: Options): String =
    if (alarms.isEmpty) "No matching keys found."
    else formatAlarms0(alarms, options)

  private def formatAlarms0(alarms: List[Alarm], options: Options): String =
    ((options.showMetadata, options.showStatus) match {
      case (true, false) ⇒ alarms.map(a ⇒ formatMetadata(a.metadata))
      case (false, true) ⇒ alarms.map(a ⇒ formatOnlyStatus(a.key, a.status, a.severity))
      case _             ⇒ alarms.map(formatAlarm)
    }).mkString(s"$Separator$Newline", s"\n$Separator$Newline", s"$Newline$Separator")

  def formatAggregatedSeverity(key: Key, severity: FullAlarmSeverity): String = msg(key, "Severity", severity.toString)
  def formatAggregatedHealth(key: Key, health: AlarmHealth): String           = msg(key, "Health", health.toString)
  def formatRefreshSeverity(key: Key, severity: FullAlarmSeverity): String    = s"Severity for [$key] refreshed to: $severity"

  def msg(key: Key, property: String, value: String): String = key match {
    case GlobalKey                          ⇒ s"Aggregated $property of Alarm Service: $value"
    case SubsystemKey(subsystem)            ⇒ s"Aggregated $property of Subsystem [$subsystem]: $value"
    case ComponentKey(subsystem, component) ⇒ s"Aggregated $property of Component [$subsystem$KeySeparator$component]: $value"
    case _: AlarmKey                        ⇒ s"$property of Alarm [$key]: $value"
  }

  private def formatAlarm(alarm: Alarm): String =
    List(formatMetadata(alarm.metadata), formatStatus(alarm.status), formatSeverity(alarm.severity)).mkString(Newline)

  private def formatMetadata(metadata: AlarmMetadata): String = {
    import metadata._

    List(
      s"Subsystem: $subsystem",
      s"Component: $component",
      s"Name: $name",
      s"Description: $description",
      s"Location: $location",
      s"Type: $alarmType",
      s"Supported Severities: ${allSupportedSeverities.mkString("[", ", ", "]")}",
      s"Probable Cause: $probableCause",
      s"Operator Response: $operatorResponse",
      s"AutoAcknowledgeable: $isAutoAcknowledgeable",
      s"Latchable: $isLatchable",
      s"Activation Status: $activationStatus"
    ).mkString(Newline)
  }

  private def formatOnlyStatus(alarmKey: AlarmKey, status: AlarmStatus, severity: FullAlarmSeverity): String =
    List(formatKey(alarmKey), formatStatus(status), formatSeverity(severity)).mkString(Newline)

  private def formatStatus(status: AlarmStatus): String = {
    import status._

    List(
      s"Acknowledgement Status: $acknowledgementStatus",
      s"Latch Severity: $latchedSeverity",
      s"Shelve Status: $shelveStatus",
      s"Alarm Time: ${alarmTime.value}"
    ).mkString(Newline)
  }

  private def formatKey(key: AlarmKey): String                    = s"Alarm Key: $key"
  private def formatSeverity(severity: FullAlarmSeverity): String = s"Current Severity: $severity"

}
