package csw.services.alarm.api.models

/**
 * A wrapper class representing the key for an alarm e.g. nfiraos.trombone.tromboneAxisLowLimitAlarm. It represents each
 * alarm uniquely.
 *
 * @param subsystem represents the subsystem of the component that raises an alarm e.g. nfiraos
 * @param component represents the component that raises an alarm e.g trombone
 * @param name represents the name of the alarm unique to the component e.g tromboneAxisLowLimitAlarm
 */
sealed abstract class Key(subsystem: String, component: String, name: String) {
  def value: String = s"$subsystem.$component.$name"
}

object Key {
  case class AlarmKey(subsystem: String, component: String, name: String) extends Key(subsystem, component, name)
  case class ComponentKey(subsystem: String, component: String)           extends Key(subsystem, component, "*")
  case class SubsystemKey(subsystem: String)                              extends Key(subsystem, "*", "*")
  case object GlobalKey                                                   extends Key("*", "*", "*")
}
