package csw.alarm.api.models

import csw.alarm.api.models.AlarmSeverity._
import csw.alarm.api.models.FullAlarmSeverity.Disconnected

// DEOPSCSW-437 : Model to represent alarm severities
class FullAlarmSeverityTest extends EnumTest(FullAlarmSeverity) {
  override val expectedValues = Set(Disconnected, Okay, Warning, Major, Indeterminate, Critical)
}
