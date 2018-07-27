package csw.services.alarm.api.models
import csw.services.alarm.api.models.AcknowledgementStatus.Acknowledged
import csw.services.alarm.api.models.AlarmSeverity.Disconnected
import csw.services.alarm.api.models.LatchStatus.UnLatched
import csw.services.alarm.api.models.ShelveStatus.UnShelved

case class AlarmStatus(
    acknowledgementStatus: AcknowledgementStatus = Acknowledged,
    latchStatus: LatchStatus = UnLatched,
    latchedSeverity: AlarmSeverity = Disconnected,
    shelveStatus: ShelveStatus = UnShelved
)
