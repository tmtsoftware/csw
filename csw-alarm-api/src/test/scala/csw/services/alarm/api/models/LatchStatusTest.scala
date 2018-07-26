package csw.services.alarm.api.models

import csw.services.alarm.api.models.LatchStatus.{Latched, UnLatched}

// DEOPSCSW-440: Model to represent Alarm Latched status
class LatchStatusTest extends EnumTest(LatchStatus) {
  override val expectedValues = Set(Latched, UnLatched)
}
