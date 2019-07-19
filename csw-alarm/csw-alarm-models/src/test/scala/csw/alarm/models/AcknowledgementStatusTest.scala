package csw.alarm.models

import csw.alarm.models.AcknowledgementStatus.{Acknowledged, Unacknowledged}

// DEOPSCSW-441: Model to represent Alarm Acknowledgement status
class AcknowledgementStatusTest extends EnumTest(AcknowledgementStatus) {
  override val expectedValues = Set(Acknowledged, Unacknowledged)
}
