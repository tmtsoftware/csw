package csw.services.alarm.client.internal.helpers

import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.api.models.{AcknowledgementStatus, AlarmSeverity, FullAlarmSeverity}

case class SetSeverityAcknowledgementTestCase(
    alarmKey: AlarmKey,
    oldSeverity: FullAlarmSeverity,
    newSeverity: AlarmSeverity,
    isAutoAcknowledgeable: Boolean,
    oldAckStatus: AcknowledgementStatus,
    newAckStatus: AcknowledgementStatus
) {
  def name: String =
    s"""ack status should transition from $oldAckStatus to $newAckStatus
       |when severity changes from $oldSeverity to $newSeverity &
       |autoAck=$isAutoAcknowledgeable
       |""".stripMargin
}
