package csw.services.alarm.client.internal.helpers

import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.api.models.{AcknowledgementStatus, AlarmSeverity, FullAlarmSeverity}

case class SetSeverityAcknowledgementTestCase(
    alarmKey: AlarmKey,
    oldSeverity: FullAlarmSeverity,
    newSeverity: AlarmSeverity,
    expectedAckStatus: Option[AcknowledgementStatus] // None stands for No change in ack status
) {
  override def toString: String = alarmKey.name
}
