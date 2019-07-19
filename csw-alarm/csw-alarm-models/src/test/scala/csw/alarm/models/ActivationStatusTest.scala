package csw.alarm.models

import csw.alarm.models.ActivationStatus.{Active, Inactive}

// DEOPSCSW-443: Model to represent Alarm Activation status
class ActivationStatusTest extends EnumTest(ActivationStatus) {
  override val expectedValues = Set(Active, Inactive)
}
