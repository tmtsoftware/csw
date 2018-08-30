package csw.services.alarm.api.internal

import csw.services.alarm.api.models.Key
import csw.services.alarm.api.models.Key.AlarmKey

import scala.language.implicitConversions

private[alarm] case class MetadataKey(value: String)

private[alarm] object MetadataKey {
  private val prefix                                          = "metadata."
  implicit def fromAlarmKey(alarmKey: Key): MetadataKey       = MetadataKey(prefix + alarmKey.value)
  implicit def toAlarmKey(metadataKey: MetadataKey): AlarmKey = AlarmKey(metadataKey.value.stripPrefix(prefix))
}
