package csw.services.alarm.api.models
import csw.services.alarm.api.models.AcknowledgementStatus.Acknowledged
import csw.services.alarm.api.models.FullAlarmSeverity.Disconnected
import csw.services.alarm.api.models.ShelveStatus.Unshelved

/**
 * Represents occasionally changing properties of the alarm e.g current acknowledgement status, latched severity, etc.
 *
 * @param acknowledgementStatus represents if the alarm is acknowledged
 * @param latchedSeverity represents worst severity alarm has seen until it was reset
 * @param shelveStatus represents if alarm is shelved. Shelved alarms also participate in aggregation of severity and health.
 * @param alarmTime represents time of recent severity change
 */
case class AlarmStatus private[alarm] (
    acknowledgementStatus: AcknowledgementStatus,
    latchedSeverity: FullAlarmSeverity,
    shelveStatus: ShelveStatus,
    alarmTime: AlarmTime
)

object AlarmStatus {
  // Default values are left out of the serialized blob by uPickle, which means default values will not be stored in alarm store.
  // This apply method is provided as a workaround to write alarm status with default values to alarm store.
  // Refer this for more details: http://www.lihaoyi.com/upickle/#Defaults
  private[alarm] def apply(): AlarmStatus = AlarmStatus(
    acknowledgementStatus = Acknowledged,
    latchedSeverity = Disconnected,
    shelveStatus = Unshelved,
    alarmTime = AlarmTime()
  )
}
