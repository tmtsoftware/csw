package csw.alarm.models

import csw.alarm.models.AlarmSeverity._

class AlarmSeverityTest extends EnumTest(AlarmSeverity) {
  override val expectedValues = Set(Indeterminate, Okay, Warning, Major, Critical)
}
