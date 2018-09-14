package csw.alarm.client.internal.helpers

import csw.alarm.api.models.Key.AlarmKey
import csw.alarm.api.models.{AcknowledgementStatus, AlarmSeverity, FullAlarmSeverity}

case class SetSeverityAckStatusTestCase(
    alarmKey: AlarmKey,
    oldSeverity: FullAlarmSeverity,
    newSeverity: AlarmSeverity,
    isAutoAcknowledgeable: Boolean,
    oldAckStatus: AcknowledgementStatus,
    newAckStatus: AcknowledgementStatus
) {
  def name: String =
    s"ack status should transition from $oldAckStatus to $newAckStatus when severity changes from $oldSeverity to $newSeverity & autoAck=$isAutoAcknowledgeable"
}
