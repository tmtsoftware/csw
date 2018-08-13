package csw.services.alarm.api.models

import csw.services.alarm.api.models.AlarmHealth._
import csw.services.alarm.api.models.AlarmSeverity._

// DEOPSCSW-458: Calculate component health based on alarm severities
class AlarmHealthTest extends EnumTest(AlarmHealth) {
  override val expectedValues = Set(Good, Ill, Bad)

  test("severities Okay, Warning should result in Good health") {
    AlarmHealth.fromSeverity(Okay) shouldBe Good
    AlarmHealth.fromSeverity(Warning) shouldBe Good
  }

  test("severity Major should result in Ill health") {
    AlarmHealth.fromSeverity(Major) shouldBe Ill
  }

  test("severities Disconnected, Indeterminate, Critical should result in Bad health") {
    AlarmHealth.fromSeverity(Disconnected) shouldBe Bad
    AlarmHealth.fromSeverity(Indeterminate) shouldBe Bad
    AlarmHealth.fromSeverity(Critical) shouldBe Bad
  }
}
