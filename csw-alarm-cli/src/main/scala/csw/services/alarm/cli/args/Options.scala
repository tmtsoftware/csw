package csw.services.alarm.cli.args
import java.nio.file.Path

import csw.messages.params.models.Subsystem
import csw.services.alarm.api.models.AlarmSeverity.Disconnected
import csw.services.alarm.api.models.Key.{AlarmKey, ComponentKey, GlobalKey, SubsystemKey}
import csw.services.alarm.api.models.{AlarmSeverity, Key}

case class Options(
    cmd: String = "",
    filePath: Option[Path] = None,
    isLocal: Boolean = false,
    reset: Boolean = false,
    maybeSubsystem: Option[Subsystem] = None,
    maybeComponent: Option[String] = None,
    maybeAlarmName: Option[String] = None,
    severity: AlarmSeverity = Disconnected
) {
  def alarmKey: AlarmKey = AlarmKey(maybeSubsystem.get, maybeComponent.get, maybeAlarmName.get)

  def key: Key = (maybeSubsystem, maybeComponent, maybeAlarmName) match {
    case (None, None, None)                                  ⇒ GlobalKey
    case (Some(subsystem), None, None)                       ⇒ SubsystemKey(subsystem)
    case (Some(subsystem), Some(component), None)            ⇒ ComponentKey(subsystem, component)
    case (Some(subsystem), Some(component), Some(alarmName)) ⇒ AlarmKey(subsystem, component, alarmName)
  }
}
