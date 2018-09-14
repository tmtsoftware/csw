package csw.alarm.api.models

import csw.alarm.api.models.AcknowledgementStatus.{Acknowledged, Unacknowledged}

// DEOPSCSW-441: Model to represent Alarm Acknowledgement status
class AcknowledgementStatusTest extends EnumTest(AcknowledgementStatus) {
  override val expectedValues = Set(Acknowledged, Unacknowledged)
}
