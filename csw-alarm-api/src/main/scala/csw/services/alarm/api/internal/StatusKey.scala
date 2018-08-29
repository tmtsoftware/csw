package csw.services.alarm.api.internal

import csw.services.alarm.api.models.Key

import scala.language.implicitConversions

private[alarm] case class AckStatusKey(value: String)

private[alarm] object AckStatusKey {
  implicit def fromAlarmKey(alarmKey: Key): AckStatusKey = AckStatusKey("ackstatus." + alarmKey.value)
}

private[alarm] case class ShelveStatusKey(value: String)

private[alarm] object ShelveStatusKey {
  implicit def fromAlarmKey(alarmKey: Key): ShelveStatusKey = ShelveStatusKey("shelvestatus." + alarmKey.value)
}

private[alarm] case class AlarmTimeKey(value: String)

private[alarm] object AlarmTimeKey {
  implicit def fromAlarmKey(alarmKey: Key): AlarmTimeKey = AlarmTimeKey("alarmtime." + alarmKey.value)
}

private[alarm] case class LatchedSeverityKey(value: String)

private[alarm] object LatchedSeverityKey {
  implicit def fromAlarmKey(alarmKey: Key): LatchedSeverityKey = LatchedSeverityKey("latchedseverity." + alarmKey.value)
}
