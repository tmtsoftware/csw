package csw.services.alarm.api.internal

import csw.services.alarm.api.models.Key

import scala.language.implicitConversions

private[alarm] case class MetadataKey(value: String)

private[alarm] object MetadataKey {
  implicit def fromAlarmKey(alarmKey: Key): MetadataKey = MetadataKey("metadata." + alarmKey.value)
}
