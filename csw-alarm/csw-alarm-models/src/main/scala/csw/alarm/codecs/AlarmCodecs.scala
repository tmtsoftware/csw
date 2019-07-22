package csw.alarm.codecs

import csw.alarm.models.AlarmSeverity
import csw.params.core.formats.CborHelpers

object AlarmCodecs {
  implicit val alarmSeverityCodec = CborHelpers.enumCodec[AlarmSeverity]
}
