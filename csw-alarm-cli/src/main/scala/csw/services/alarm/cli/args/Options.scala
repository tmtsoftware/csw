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
    component: String = "",
    name: String = "",
    severity: AlarmSeverity = Disconnected
) {
  def alarmKey: AlarmKey = AlarmKey(maybeSubsystem.get, component, name)

  def key: Key = (maybeSubsystem, component, name) match {
    case (None, "", "")            ⇒ GlobalKey
    case (Some(subsystem), "", "") ⇒ SubsystemKey(subsystem)
    case (Some(subsystem), _, "")  ⇒ ComponentKey(subsystem, component)
    case (Some(subsystem), _, _)   ⇒ AlarmKey(subsystem, component, name)
  }
}
