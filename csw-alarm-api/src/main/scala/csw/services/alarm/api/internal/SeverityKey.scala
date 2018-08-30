package csw.services.alarm.api.internal

import csw.services.alarm.api.models.Key
import csw.services.alarm.api.internal.Separators.KeySeparator

import scala.language.implicitConversions

private[alarm] case class SeverityKey(value: String)

private[alarm] object SeverityKey {
  implicit def fromAlarmKey(alarmKey: Key): SeverityKey = SeverityKey(s"severity$KeySeparator" + alarmKey.value)

  def fromMetadataKey(metadataKey: MetadataKey): SeverityKey =
    SeverityKey(s"severity$KeySeparator" + metadataKey.value.stripPrefix(s"metadata$KeySeparator"))
}
