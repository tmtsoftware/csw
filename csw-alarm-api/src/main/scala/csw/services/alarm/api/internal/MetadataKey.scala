package csw.services.alarm.api.internal

import csw.services.alarm.api.models.Key

import scala.language.implicitConversions

case class MetadataKey(key: String)

object MetadataKey {
  implicit def fromAlarmKey(alarmKey: Key): MetadataKey = MetadataKey("metadata." + alarmKey.value)
}
