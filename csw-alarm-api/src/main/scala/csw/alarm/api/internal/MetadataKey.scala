package csw.alarm.api.internal

import csw.alarm.api.models.Key.AlarmKey
import csw.alarm.api.internal.Separators.KeySeparator
import csw.alarm.api.models.Key

import scala.language.implicitConversions

private[alarm] case class MetadataKey(value: String)

private[alarm] object MetadataKey {
  private val prefix                                          = s"metadata$KeySeparator"
  implicit def fromAlarmKey(alarmKey: Key): MetadataKey       = MetadataKey(prefix + alarmKey.value)
  implicit def toAlarmKey(metadataKey: MetadataKey): AlarmKey = AlarmKey(metadataKey.value.stripPrefix(prefix))
}
