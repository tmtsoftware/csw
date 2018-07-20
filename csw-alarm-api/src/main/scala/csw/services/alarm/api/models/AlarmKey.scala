package csw.services.alarm.api.models

import csw.services.alarm.api.models.AlarmKey.SEPARATOR

/**
 * A wrapper class representing the key for an alarm e.g. nfiraos.trombone.tromboneAxisLowLimitAlarm. It represents each
 * alarm uniquely.
 *
 * @param subsystem represents the subsystem of the component that raises an alarm e.g. nfiraos
 * @param component represents the component that raises an alarm e.g trombone
 * @param name represents the name of the alarm unique to the component e.g tromboneAxisLowLimitAlarm
 */
case class AlarmKey(subsystem: String, component: String, name: String) {
  val key                       = s"$subsystem$SEPARATOR$component$SEPARATOR$name"
  override def toString: String = key
}

object AlarmKey {
  val SEPARATOR = "."

  /**
   * Create AlarmKey from the given string representation of the same
   *
   * @param alarmKeyStr represents the string version (combination of `subsystem`, `component` and `name`) of AlarmKey
   *                    e.g. nfiraos.trombone.tromboneAxisLowLimitAlarm
   * @return an AlarmKey instance
   */
  def apply(alarmKeyStr: String): AlarmKey = {
    require(alarmKeyStr != null)
    val subsystem :: component :: name :: _ = alarmKeyStr.split(SEPARATOR).toList
    AlarmKey(subsystem, component, name)
  }
}
