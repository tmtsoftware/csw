package csw.services.alarm.api.models

import csw.services.alarm.api.models.ExplicitAlarmSeverity._

class ExplicitAlarmSeverityTest extends EnumTest(ExplicitAlarmSeverity) {
  override val expectedValues = Set(Indeterminate, Okay, Warning, Major, Critical)
}
