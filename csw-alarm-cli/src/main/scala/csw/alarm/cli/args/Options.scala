package csw.alarm.cli.args
import java.nio.file.Path

import csw.params.core.models.Subsystem
import csw.alarm.api.models.{AlarmSeverity, Key}
import csw.alarm.api.models.Key.{AlarmKey, ComponentKey, GlobalKey, SubsystemKey}

case class Options(
    cmd: String = "",
    subCmd: String = "",
    filePath: Option[Path] = None,
    isLocal: Boolean = false,
    reset: Boolean = false,
    maybeSubsystem: Option[Subsystem] = None,
    maybeComponent: Option[String] = None,
    maybeAlarmName: Option[String] = None,
    severity: Option[AlarmSeverity] = None,
    autoRefresh: Boolean = false,
    showMetadata: Boolean = true,
    showStatus: Boolean = true,
    locationHost: String = "localhost"
) {

  def alarmKey: AlarmKey = (maybeSubsystem, maybeComponent, maybeAlarmName) match {
    case (Some(subsystem), Some(component), Some(name)) ⇒ AlarmKey(subsystem, component, name)
    case _                                              ⇒ throw new IllegalArgumentException("Subsystem, Component or Alarm Name required.")
  }

  def key: Key = (maybeSubsystem, maybeComponent, maybeAlarmName) match {
    case (None, None, None)                       ⇒ GlobalKey
    case (Some(subsystem), None, None)            ⇒ SubsystemKey(subsystem)
    case (Some(subsystem), Some(component), None) ⇒ ComponentKey(subsystem, component)
    case _                                        ⇒ alarmKey
  }
}
