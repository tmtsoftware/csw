package csw.services.alarm.api.models

import csw.services.alarm.api.models.ShelveStatus.{Shelved, UnShelved}

// DEOPSCSW-442: Model to represent Alarm Shelve status
class ShelveStatusTest extends EnumTest(ShelveStatus) {
  override val expectedValues = Set(Shelved, UnShelved)
}
