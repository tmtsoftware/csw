package csw.services.alarm.api.internal

import csw.services.alarm.api.models.AlarmKey

import scala.language.implicitConversions

case class SeverityKey(key: String)

object SeverityKey {
  implicit def fromAlarmKey(alarmKey: AlarmKey): SeverityKey = SeverityKey("severity." + alarmKey.name)
}
