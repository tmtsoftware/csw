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
  private val SEPARATOR = "."
  val key               = s"${subsystem.toLowerCase}$SEPARATOR${component.toLowerCase}$SEPARATOR${name.toLowerCase}"
}

object AlarmKey {
  private[alarm] def withPattern(subsystem: Option[String], component: Option[String], alarmName: Option[String]): AlarmKey = {
    val WILD_CARD = "*"
    AlarmKey(subsystem.getOrElse(WILD_CARD), component.getOrElse(WILD_CARD), alarmName.getOrElse(WILD_CARD))
  }

  private[alarm] def apply(alarmKeyStr: String): AlarmKey = {
    val subsystem :: component :: name :: _ = alarmKeyStr.split(".").toList
    AlarmKey(subsystem, component, name)
  }
}
