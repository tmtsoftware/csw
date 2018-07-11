package csw.services.alarm.api.models

import csw.services.alarm.api.internal.UPickleFormatAdapter
import play.api.libs.json.{Json, OFormat}
import upickle.default.{ReadWriter ⇒ RW}

case class AlarmStatus(
    acknowledgementStatus: AcknowledgementStatus,
    latchStatus: LatchStatus,
    latchedSeverity: AlarmSeverity,
    shelveStatus: ShelveStatus,
    activationStatus: ActivationStatus
)

object AlarmStatus {
  implicit val format: OFormat[AlarmStatus] = Json.format[AlarmStatus]
  implicit val alarmKeyRw: RW[AlarmStatus]  = UPickleFormatAdapter.playJsonToUPickle
}
