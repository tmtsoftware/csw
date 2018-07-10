package csw.services.alarm.api.models

import csw.messages.params.models.Prefix
import csw.services.alarm.api.internal.UPickleFormatAdapter
import play.api.libs.json.{Json, OFormat}
import upickle.default.{ReadWriter => RW, _}

/**
 * A wrapper class representing the key for an alarm e.g. nfiraos.cc.trombone.tromboneAxisLowLimitAlarm. It represents each
 * alarm uniquely.
 *
 * @param source represents the prefix of the component that raises an alarm e.g. nfiraos.cc.trombone
 * @param alarmName represents the name of the alarm e.g tromboneAxisLowLimitAlarm
 */
case class AlarmKey(source: Prefix, alarmName: AlarmName) {
  val key                       = s"${source.prefix}.$alarmName"
  override def toString: String = key
}

object AlarmKey {
  private val SEPARATOR = "."

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

  implicit val format: OFormat[AlarmKey] = Json.format[AlarmKey]
  implicit val alarmKeyRw: RW[AlarmKey]  = UPickleFormatAdapter.playJsonToUPickle
}
