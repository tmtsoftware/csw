package csw.alarm.models

import csw.alarm.models.ShelveStatus.{Shelved, Unshelved}

// DEOPSCSW-442: Model to represent Alarm Shelve status
class ShelveStatusTest extends EnumTest(ShelveStatus) {
  override val expectedValues = Set(Shelved, Unshelved)
}
