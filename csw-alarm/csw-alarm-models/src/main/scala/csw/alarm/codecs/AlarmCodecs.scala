package csw.alarm.codecs

import csw.alarm.models.AlarmSeverity
import csw.params.core.formats.CodecHelpers
import io.bullet.borer.Codec

object AlarmCodecs {
  implicit val alarmSeverityCodec: Codec[AlarmSeverity] = CodecHelpers.enumCodec[AlarmSeverity]
}
