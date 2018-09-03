package csw.services.alarm.api.models
import java.util.regex.Pattern

import csw.messages.params.models.Subsystem
import csw.services.alarm.api.internal.RichStringExtentions.RichString
import csw.services.alarm.api.internal.Separators.KeySeparator

/**
 * A wrapper class representing the key for an alarm e.g. nfiraos.trombone.tromboneaxislowlimitalarm
 *
 * @note key is case-insensitive e.g nfiraos.trombone.tromboneaxislowlimitalarm and
 * NFIRAOS.trombone.tromboneaxislowlimitalarm keys are equal
 * @param subsystem represents the subsystem of the component that raises an alarm e.g. nfiraos
 * @param component represents the component that raises an alarm e.g trombone
 * @param name represents the name of the alarm unique to the component e.g tromboneAxisLowLimitAlarm
 */
sealed abstract class Key(subsystem: String, component: String, name: String) extends Proxy {
  require(component.isDefined, "component should not be an empty value")
  require(name.isDefined, "name should not be an empty value")

  /**
   * Unique value of the key which is combination of subsystem, component and name
   */
  val value: String = s"$subsystem$KeySeparator$component$KeySeparator$name".toLowerCase

  override def self: Any = value
}

object Key {
  // pattern matches for any one of *, [, ], ^, - characters  present
  val invalidChars: Pattern = Pattern.compile(".*[\\*\\[\\]\\^\\?\\-].*")

  /**
   * Represents unique alarm in the given subsystem and component
   *
   * @param subsystem represents the subsystem of the component to which that alarm belongs e.g. nfiraos
   * @param component represents the component to which that alarm belongs e.g trombone
   * @param name represents the name of the alarm unique to the component e.g tromboneAxisLowLimitAlarm
   */
  case class AlarmKey(subsystem: Subsystem, component: String, name: String) extends Key(subsystem.name, component, name) {
    require(!component.matches(invalidChars), "key contains invalid characters")
    require(!name.matches(invalidChars), "key contains invalid characters")
  }

  /**
   * Represents all the alarms present given subsystem and component
   *
   * @param subsystem represents the subsystem of the component that raises an alarm e.g. nfiraos
   * @param component represents the component whose alarms to be included e.g trombone
   */
  case class ComponentKey(subsystem: Subsystem, component: String) extends Key(subsystem.name, component, "*") {
    require(!component.matches(invalidChars), "component name contains invalid characters")
  }

  /**
   *Represents all the alarm present given subsystem
   *
   * @param subsystem represents the subsystem whose alarms to be included e.g. nfiraos
   */
  case class SubsystemKey(subsystem: Subsystem) extends Key(subsystem.name, "*", "*")

  /**
   * Represents all the alarms available in the system.
   */
  case object GlobalKey extends Key("*", "*", "*")

  object AlarmKey {
    private[alarm] def apply(keyStr: String): AlarmKey = keyStr.split(KeySeparator) match {
      case Array(subsystem, component, name) ⇒ AlarmKey(Subsystem.withName(subsystem), component, name)
      case _                                 ⇒ throw new IllegalArgumentException(s"Unable to parse '$keyStr' to make AlarmKey object")
    }
  }
}
