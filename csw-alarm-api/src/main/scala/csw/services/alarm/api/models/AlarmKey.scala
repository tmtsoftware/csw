package csw.services.alarm.api.models

/**
 * A wrapper class representing the key for an alarm e.g. nfiraos.trombone.tromboneAxisLowLimitAlarm. It represents each
 * alarm uniquely.
 *
 * @param subsystem represents the subsystem of the component that raises an alarm e.g. nfiraos
 * @param component represents the component that raises an alarm e.g trombone
 * @param name represents the name of the alarm unique to the component e.g tromboneAxisLowLimitAlarm
 */
case class AlarmKey(subsystem: String, component: String, name: String) {
  private val SEPARATOR         = "."
  private val key               = s"$subsystem$SEPARATOR$component$SEPARATOR$name"
  override def toString: String = key
}
