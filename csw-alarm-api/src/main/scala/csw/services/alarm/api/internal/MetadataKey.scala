package csw.services.alarm.api.internal

import csw.services.alarm.api.models.AlarmKey

import scala.language.implicitConversions

case class MetadataKey(key: String)

object MetadataKey {
  implicit def fromAlarmKey(alarmKey: AlarmKey): MetadataKey = MetadataKey("metadata." + alarmKey.name)
}
