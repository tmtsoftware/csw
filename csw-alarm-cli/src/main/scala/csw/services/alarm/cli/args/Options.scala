package csw.services.alarm.cli.args
import java.nio.file.Path

import csw.services.alarm.api.models.AlarmSeverity
import csw.services.alarm.api.models.AlarmSeverity.Disconnected
import csw.services.alarm.api.models.Key.AlarmKey

case class Options(
    cmd: String = "",
    filePath: Option[Path] = None,
    isLocal: Boolean = false,
    reset: Boolean = false,
    subsystem: String = "",
    component: String = "",
    name: String = "",
    severity: AlarmSeverity = Disconnected
) {
  def alarmKey: AlarmKey = AlarmKey(subsystem, component, name)
}
