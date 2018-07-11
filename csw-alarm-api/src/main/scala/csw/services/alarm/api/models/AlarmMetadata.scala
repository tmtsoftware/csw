package csw.services.alarm.api.models

import csw.services.alarm.api.internal.UPickleFormatAdapter
import play.api.libs.json.{Json, OFormat}
import upickle.default.{ReadWriter ⇒ RW}

case class AlarmMetadata(
    alarmKey: AlarmKey,
    description: String,
    location: String,
    alarmType: AlarmType,
    supportedSeverityLevels: List[AlarmSeverity],
    probableCause: String,
    operatorResponse: String,
    autoAcknowledgable: Boolean,
    isLatchable: Boolean
)

object AlarmMetadata {
  implicit val format: OFormat[AlarmMetadata] = Json.format[AlarmMetadata]
  implicit val alarmKeyRw: RW[AlarmMetadata]  = UPickleFormatAdapter.playJsonToUPickle
}
