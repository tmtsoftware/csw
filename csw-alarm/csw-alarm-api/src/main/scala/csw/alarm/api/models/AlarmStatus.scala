package csw.alarm.api.models
import csw.alarm.api.models.AcknowledgementStatus.Acknowledged
import csw.alarm.api.models.FullAlarmSeverity.Disconnected
import csw.alarm.api.models.ShelveStatus.Unshelved
import csw.time.core.models.UTCTime

/**
 * Represents occasionally changing properties of the alarm e.g current acknowledgement status, latched severity, etc.
 *
 * @note acknowledgement status, alarm time and latched severity changes based on severity change of an alarm
 */
case class AlarmStatus private[alarm] (
    acknowledgementStatus: AcknowledgementStatus,
    latchedSeverity: FullAlarmSeverity,
    shelveStatus: ShelveStatus,
    alarmTime: UTCTime
)

object AlarmStatus {
  // Default values are left out of the serialized blob by uPickle, which means default values will not be stored in alarm store.
  // So this apply method is provided as a workaround to write alarm status with default values to alarm store.
  // Refer this for more details: http://www.lihaoyi.com/upickle/#Defaults
  private[alarm] def apply(): AlarmStatus = AlarmStatus(
    acknowledgementStatus = Acknowledged,
    latchedSeverity = Disconnected,
    shelveStatus = Unshelved,
    alarmTime = UTCTime.now()
  )
}
