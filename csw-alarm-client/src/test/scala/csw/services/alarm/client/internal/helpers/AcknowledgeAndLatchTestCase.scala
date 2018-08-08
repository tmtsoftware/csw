package csw.services.alarm.client.internal.helpers

import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.api.models.{AcknowledgementStatus, AlarmSeverity, LatchStatus}

case class AcknowledgeAndLatchTestCase(
    alarmKey: AlarmKey,
    isAutoAcknowledgeble: Boolean,
    isAlarmLachable: Boolean,
    oldSeverity: AlarmSeverity,
    newSeverity: AlarmSeverity,
    expectedAckStatus: AcknowledgementStatus,
    expectedLatchStatus: LatchStatus,
) {
  override def toString: String = alarmKey.name
}
