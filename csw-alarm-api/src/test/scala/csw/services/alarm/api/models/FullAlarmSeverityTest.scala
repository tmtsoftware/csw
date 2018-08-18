package csw.services.alarm.api.models

import csw.services.alarm.api.models.FullAlarmSeverity.Disconnected
import csw.services.alarm.api.models.AlarmSeverity._

// DEOPSCSW-437 : Model to represent alarm severities
class FullAlarmSeverityTest extends EnumTest(FullAlarmSeverity) {
  override val expectedValues = Set(Disconnected, Okay, Warning, Major, Indeterminate, Critical)
}
