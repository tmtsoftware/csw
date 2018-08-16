package csw.services.alarm.api.models
import csw.services.alarm.api.models.AcknowledgementStatus.Unacknowledged
import csw.services.alarm.api.models.AlarmSeverity.Disconnected
import csw.services.alarm.api.models.LatchStatus.UnLatched
import csw.services.alarm.api.models.ShelveStatus.Unshelved

case class AlarmStatus(
    acknowledgementStatus: AcknowledgementStatus = Unacknowledged,
    latchStatus: LatchStatus = UnLatched,
    latchedSeverity: AlarmSeverity = Disconnected,
    shelveStatus: ShelveStatus = Unshelved,
    alarmTime: Option[AlarmTime] = None
)
