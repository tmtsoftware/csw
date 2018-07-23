package csw.services.alarm.api.internal

import csw.services.alarm.api.models.AlarmKey

import scala.language.implicitConversions

case class StatusKey(key: String)

object StatusKey {
  implicit def fromAlarmKey(alarmKey: AlarmKey): StatusKey = StatusKey("status." + alarmKey.key)
}
