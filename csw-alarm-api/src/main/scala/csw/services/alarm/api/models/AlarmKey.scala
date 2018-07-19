package csw.services.alarm.api.models

import csw.messages.params.models.Prefix

import scala.language.implicitConversions

/**
 * A wrapper class representing the key for an alarm e.g. nfiraos.cc.trombone.tromboneAxisLowLimitAlarm. It represents each
 * alarm uniquely.
 *
 * @param source represents the prefix of the component that raises an alarm e.g. nfiraos.cc.trombone
 * @param alarmName represents the name of the alarm e.g tromboneAxisLowLimitAlarm
 */
case class AlarmKey(source: Prefix, alarmName: AlarmName) {
  val name                      = s"${source.prefix}.$alarmName"
  override def toString: String = name
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
}

case class MetadataKey(key: String)
object MetadataKey {
  implicit def fromAlarmKey(alarmKey: AlarmKey): MetadataKey = MetadataKey("metadata." + alarmKey.name)
}

case class StatusKey(key: String)
object StatusKey {
  implicit def fromAlarmKey(alarmKey: AlarmKey): StatusKey = StatusKey("status." + alarmKey.name)
}

case class SeverityKey(key: String)
object SeverityKey {
  implicit def fromAlarmKey(alarmKey: AlarmKey): SeverityKey = SeverityKey("severity." + alarmKey.name)
}
