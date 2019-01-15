package csw.alarm.api.models

import csw.alarm.api.models.ShelveStatus.{Shelved, Unshelved}

// DEOPSCSW-442: Model to represent Alarm Shelve status
class ShelveStatusTest extends EnumTest(ShelveStatus) {
  override val expectedValues = Set(Shelved, Unshelved)
}
