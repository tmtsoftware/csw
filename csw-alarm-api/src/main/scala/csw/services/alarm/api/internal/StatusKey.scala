package csw.services.alarm.api.internal

import csw.services.alarm.api.models.Key

import scala.language.implicitConversions

case class StatusKey(value: String)

object StatusKey {
  implicit def fromAlarmKey(alarmKey: Key): StatusKey = StatusKey("status." + alarmKey.value)
}
