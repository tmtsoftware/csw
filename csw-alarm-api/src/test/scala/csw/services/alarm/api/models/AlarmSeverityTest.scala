package csw.services.alarm.api.models

import csw.services.alarm.api.models.AlarmSeverity.Disconnected
import csw.services.alarm.api.models.ExplicitAlarmSeverity._

// DEOPSCSW-437 : Model to represent alarm severities
class AlarmSeverityTest extends EnumTest(AlarmSeverity) {
  override val expectedValues = Set(Disconnected, Okay, Warning, Major, Indeterminate, Critical)
}
