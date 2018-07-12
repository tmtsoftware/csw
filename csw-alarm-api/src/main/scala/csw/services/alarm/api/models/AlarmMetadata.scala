package csw.services.alarm.api.models

import csw.services.alarm.api.internal.UPickleFormatAdapter
import play.api.libs.json.{Json, OFormat}
import upickle.default.{ReadWriter ⇒ RW}

case class AlarmMetadata(
    alarmKey: AlarmKey,
    description: String,
    location: String,
    alarmType: AlarmType,
    supportedSeverities: List[AlarmSeverity],
    probableCause: String,
    operatorResponse: String,
    isAutoAcknowledgable: Boolean,
    isLatchable: Boolean
)

object AlarmMetadata {
  implicit val format: OFormat[AlarmMetadata] = Json.format[AlarmMetadata]
  implicit val alarmKeyRw: RW[AlarmMetadata]  = UPickleFormatAdapter.playJsonToUPickle
}
