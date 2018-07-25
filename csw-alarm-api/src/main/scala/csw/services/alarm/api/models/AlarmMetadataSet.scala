package csw.services.alarm.api.models
import csw.services.alarm.api.internal.AlarmRW
import upickle.default.{macroRW, ReadWriter ⇒ RW}

case class AlarmMetadataSet(alarms: Set[AlarmMetadata])

object AlarmMetadataSet extends AlarmRW {
  implicit val alarmMetadataSetRW: RW[AlarmMetadataSet] = macroRW
}
