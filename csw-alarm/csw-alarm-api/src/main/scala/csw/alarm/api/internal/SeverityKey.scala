package csw.alarm.api.internal

import csw.alarm.commons.Separators.KeySeparator
import csw.alarm.models.Key
import csw.alarm.models.Key.AlarmKey

import scala.language.implicitConversions

private[alarm] case class SeverityKey(value: String)

private[alarm] object SeverityKey {
  private val prefix = s"severity$KeySeparator"
  implicit def toAlarmKey(severityKey: SeverityKey): AlarmKey = AlarmKey(severityKey.value.stripPrefix(prefix))
  implicit def fromAlarmKey(key: Key): SeverityKey = SeverityKey(prefix + key.value)
}
