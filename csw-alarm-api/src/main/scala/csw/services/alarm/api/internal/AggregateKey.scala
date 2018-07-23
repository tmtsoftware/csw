package csw.services.alarm.api.internal

import csw.services.alarm.api.models.AlarmKey

import scala.language.implicitConversions

case class AggregateKey(key: String)

object AggregateKey {
  implicit def fromAlarmKey(alarmKey: AlarmKey): AggregateKey =
    AggregateKey("__keyspace@0__" + StatusKey.fromAlarmKey(alarmKey).key)
}
