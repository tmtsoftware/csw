package csw.services.alarm.api.models

import csw.services.alarm.api.models.AlarmSeverity._

class AlarmSeverityTest extends EnumTest(AlarmSeverity) {
  override val expectedValues = Set(Indeterminate, Okay, Warning, Major, Critical)
}
