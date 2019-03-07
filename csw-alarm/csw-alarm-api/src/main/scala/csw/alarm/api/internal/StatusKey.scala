package csw.alarm.api.internal

import csw.alarm.api.internal.Separators.KeySeparator
import csw.alarm.api.models.Key

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

private[alarm] case class InitializingKey(value: String)

private[alarm] object InitializingKey {
  implicit def fromAlarmKey(alarmKey: Key): InitializingKey =
    InitializingKey(s"initializing$KeySeparator" + alarmKey.value)
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
