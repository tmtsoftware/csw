package csw.services.alarm.api.models

import csw.messages.params.models.Prefix
import csw.services.alarm.api.internal.UPickleFormatAdapter
import csw.services.alarm.api.models.AlarmKey.{METADATA_KEY_SUFFIX, SEVERITY_KEY_SUFFIX, STATUS_KEY_SUFFIX}
import play.api.libs.json.{Json, OFormat}
import upickle.default.{ReadWriter ⇒ RW}

/**
 * A wrapper class representing the key for an alarm e.g. nfiraos.cc.trombone.tromboneAxisLowLimitAlarm. It represents each
 * alarm uniquely.
 *
 * @param source represents the prefix of the component that raises an alarm e.g. nfiraos.cc.trombone
 * @param alarmName represents the name of the alarm e.g tromboneAxisLowLimitAlarm
 */
case class AlarmKey(source: Prefix, alarmName: AlarmName) {
  val name = s"${source.prefix}.$alarmName"

  private[alarm] val metadataKey: String = name + METADATA_KEY_SUFFIX
  private[alarm] val statusKey: String   = name + STATUS_KEY_SUFFIX
  private[alarm] val severityKey: String = name + SEVERITY_KEY_SUFFIX

  override def toString: String = name
}

object AlarmKey {
  private val SEPARATOR           = "."
  private val METADATA_KEY_SUFFIX = ".metadata"
  private val STATUS_KEY_SUFFIX   = ".status"
  private val SEVERITY_KEY_SUFFIX = ".severity"

  /**
   * Create AlarmKey from the given string representation of the same
   *
   * @param alarmKeyStr represents the string version (combination of `Prefix` and `AlarmName`) of AlarmKey
   *                    e.g. nfiraos.cc.trombone.tromboneAxisLowLimitAlarm
   * @return an AlarmKey instance
   */
  def apply(alarmKeyStr: String): AlarmKey = {
    require(alarmKeyStr != null)
    val strings = alarmKeyStr.splitAt(alarmKeyStr.lastIndexOf(SEPARATOR))
    new AlarmKey(Prefix(strings._1), AlarmName(strings._2.tail))
  }

  private[alarm] def fromMetadataKey(metadataKeyStr: String): AlarmKey = apply(metadataKeyStr.replace(METADATA_KEY_SUFFIX, ""))
  private[alarm] def fromStatusKey(statusKeyStr: String): AlarmKey     = apply(statusKeyStr.replace(STATUS_KEY_SUFFIX, ""))
  private[alarm] def fromSeverityKey(severityKeyStr: String): AlarmKey = apply(severityKeyStr.replace(SEVERITY_KEY_SUFFIX, ""))

  implicit val format: OFormat[AlarmKey] = Json.format[AlarmKey]
  implicit val alarmKeyRw: RW[AlarmKey]  = UPickleFormatAdapter.playJsonToUPickle
}
