package csw.services.alarm.api.internal

import csw.services.alarm.api.models.Key
import csw.services.alarm.api.internal.Separators.KeySeparator

import scala.language.implicitConversions

private[alarm] case class AckStatusKey(value: String)

private[alarm] object AckStatusKey {
  implicit def fromAlarmKey(alarmKey: Key): AckStatusKey = AckStatusKey(s"ackstatus$KeySeparator" + alarmKey.value)
}

private[alarm] case class ShelveStatusKey(value: String)

private[alarm] object ShelveStatusKey {
  implicit def fromAlarmKey(alarmKey: Key): ShelveStatusKey =
    ShelveStatusKey(s"shelvestatus$KeySeparator" + alarmKey.value)
}

private[alarm] case class AlarmTimeKey(value: String)

private[alarm] object AlarmTimeKey {
  implicit def fromAlarmKey(alarmKey: Key): AlarmTimeKey = AlarmTimeKey(s"alarmtime$KeySeparator" + alarmKey.value)
}

private[alarm] case class LatchedSeverityKey(value: String)

private[alarm] object LatchedSeverityKey {
  implicit def fromAlarmKey(alarmKey: Key): LatchedSeverityKey =
    LatchedSeverityKey(s"latchedseverity$KeySeparator" + alarmKey.value)
}
