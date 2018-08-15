package csw.services.alarm.api.models
import java.util.regex.Pattern

import csw.services.alarm.api.internal.RichStringExtentions.RichString

/**
 * A wrapper class representing the key for an alarm e.g. nfiraos.trombone.tromboneaxislowlimitalarm. It represents each
 * alarm uniquely. Note that an alarm key is case-insensitive which means that nfiraos.trombone.tromboneaxislowlimitalarm and
 * NFIRAOS.trombone.tromboneaxislowlimitalarm keys are equal.
 *
 * @param subsystem represents the subsystem of the component that raises an alarm e.g. nfiraos
 * @param component represents the component that raises an alarm e.g trombone
 * @param name represents the name of the alarm unique to the component e.g tromboneAxisLowLimitAlarm
 */
sealed abstract class Key(subsystem: String, component: String, name: String) extends Proxy {
  require(subsystem.isDefined, "subsystem should not be an empty value")
  require(component.isDefined, "component should not be an empty value")
  require(name.isDefined, "name should not be an empty value")

  val value: String      = s"$subsystem.$component.$name".toLowerCase
  override def self: Any = value
}

object Key {
  // pattern matches for any one of *, [, ], ^, - characters  present
  val invalidChars: Pattern = Pattern.compile(".*[\\*\\[\\]\\^\\?\\-].*")

  case class AlarmKey(subsystem: String, component: String, name: String) extends Key(subsystem, component, name) {
    require(!value.matches(invalidChars), "key contains invalid characters")
  }

  case class ComponentKey(subsystem: String, component: String) extends Key(subsystem, component, "*") {
    require(!subsystem.matches(invalidChars), "subsystem name contains invalid characters")
    require(!component.matches(invalidChars), "component name contains invalid characters")
  }

  case class SubsystemKey(subsystem: String) extends Key(subsystem, "*", "*") {
    require(!subsystem.matches(invalidChars), "subsystem name contains invalid characters")
  }

  case object GlobalKey extends Key("*", "*", "*")
}
