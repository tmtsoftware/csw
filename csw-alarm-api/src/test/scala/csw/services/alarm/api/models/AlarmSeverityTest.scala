package csw.services.alarm.api.models

import csw.services.alarm.api.models.AlarmSeverity._

// DEOPSCSW-437 : Model to represent alarm severities
class AlarmSeverityTest extends EnumTest(AlarmSeverity) {
  override val expectedValues = Set(Disconnected, Indeterminate, Okay, Warning, Major, Critical)
}
