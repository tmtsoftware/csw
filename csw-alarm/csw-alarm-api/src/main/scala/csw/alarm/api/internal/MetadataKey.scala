package csw.alarm.api.internal

import csw.alarm.commons.Separators.KeySeparator
import csw.alarm.models.Key
import csw.alarm.models.Key.AlarmKey

import scala.language.implicitConversions

private[alarm] case class MetadataKey(value: String)

private[alarm] object MetadataKey {
  private val prefix                                          = s"metadata$KeySeparator"
  implicit def fromAlarmKey(alarmKey: Key): MetadataKey       = MetadataKey(prefix + alarmKey.value)
  implicit def toAlarmKey(metadataKey: MetadataKey): AlarmKey = AlarmKey(metadataKey.value.stripPrefix(prefix))
}
