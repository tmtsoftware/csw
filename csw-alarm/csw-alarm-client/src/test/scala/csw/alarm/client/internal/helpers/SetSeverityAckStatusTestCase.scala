package csw.alarm.client.internal.helpers

import csw.alarm.models.Key.AlarmKey
import csw.alarm.models.{AcknowledgementStatus, AlarmSeverity, FullAlarmSeverity}

case class SetSeverityAckStatusTestCase(
    alarmKey: AlarmKey,
    oldSeverity: FullAlarmSeverity,
    newSeverity: AlarmSeverity,
    isAutoAcknowledgeable: Boolean,
    oldAckStatus: AcknowledgementStatus,
    newAckStatus: AcknowledgementStatus
) {
  def name(severity: FullAlarmSeverity = newSeverity): String =
    s"ack status should transition from $oldAckStatus to $newAckStatus when severity changes from $oldSeverity to $severity & autoAck=$isAutoAcknowledgeable"
}
