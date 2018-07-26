package csw.services.alarm.api.models

case class AlarmStatus(
    acknowledgementStatus: AcknowledgementStatus,
    latchStatus: LatchStatus,
    latchedSeverity: AlarmSeverity,
    shelveStatus: ShelveStatus
)
