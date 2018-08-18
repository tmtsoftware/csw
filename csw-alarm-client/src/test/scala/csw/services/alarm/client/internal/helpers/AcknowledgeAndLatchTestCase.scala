package csw.services.alarm.client.internal.helpers

import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.api.models.{AcknowledgementStatus, AlarmSeverity, FullAlarmSeverity}

case class AcknowledgeAndLatchTestCase(
    alarmKey: AlarmKey,
    isAutoAcknowledgeble: Boolean,
    isAlarmLachable: Boolean,
    oldSeverity: FullAlarmSeverity,
    newSeverity: AlarmSeverity,
    expectedAckStatus: AcknowledgementStatus
) {
  override def toString: String = alarmKey.name
}
