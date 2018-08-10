package csw.services.alarm.api.internal

import csw.services.alarm.api.models.Key

import scala.language.implicitConversions

case class SeverityKey(value: String)

object SeverityKey {
  implicit def fromAlarmKey(alarmKey: Key): SeverityKey = SeverityKey("severity." + alarmKey.value)

  def fromMetadataKey(metadataKey: MetadataKey): SeverityKey = SeverityKey(metadataKey.value.replace("metadata.", "severity."))
}
