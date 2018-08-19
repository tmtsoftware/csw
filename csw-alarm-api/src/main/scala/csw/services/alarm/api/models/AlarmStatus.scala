package csw.services.alarm.api.models
import csw.services.alarm.api.models.AcknowledgementStatus.Acknowledged
import csw.services.alarm.api.models.FullAlarmSeverity.Disconnected
import csw.services.alarm.api.models.ShelveStatus.Unshelved

case class AlarmStatus(
    acknowledgementStatus: AcknowledgementStatus,
    latchedSeverity: FullAlarmSeverity,
    shelveStatus: ShelveStatus,
    alarmTime: Option[AlarmTime]
)

object AlarmStatus {
  // Default values are left out of the serialized blob by uPickle, which means default values will not be stored in alarm store.
  // This apply method is provided as a workaround to write alarm status with default values to alarm store.
  // Refer this for more details: http://www.lihaoyi.com/upickle/#Defaults
  def apply(): AlarmStatus = AlarmStatus(
    acknowledgementStatus = Acknowledged,
    latchedSeverity = Disconnected,
    shelveStatus = Unshelved,
    alarmTime = None
  )
}
