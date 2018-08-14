package csw.services.alarm.api.models
import java.util.regex.Pattern

/**
 * A wrapper class representing the key for an alarm e.g. NFIRAOS.trombone.tromboneAxisLowLimitAlarm. It represents each
 * alarm uniquely.
 *
 * @param subsystem represents the subsystem of the component that raises an alarm e.g. NFIRAOS
 * @param component represents the component that raises an alarm e.g trombone
 * @param name represents the name of the alarm unique to the component e.g tromboneAxisLowLimitAlarm
 */
sealed abstract class Key(subsystem: String, component: String, name: String) extends Proxy {
  require(!isNullOrEmpty(subsystem), "subsystem should not be an empty value")
  require(!isNullOrEmpty(component), "component should not be an empty value")
  require(!isNullOrEmpty(name), "name should not be an empty value")

  val value: String      = s"$subsystem.$component.$name".toLowerCase
  override def self: Any = value

  private def isNullOrEmpty(value: String) = value == null || value.isEmpty
}

object Key {
  // pattern matches for any one of *, [, ], ^, - characters  present
  val patternForInvalidKey: Pattern = Pattern.compile(".*[\\*\\[\\]\\^\\?\\-].*")

  case class AlarmKey(subsystem: String, component: String, name: String) extends Key(subsystem, component, name) {
    require(!patternForInvalidKey.matcher(value).matches(), "key contains invalid characters")
  }

  object AlarmKey {
    def apply(key: String): AlarmKey = {
      val keyParts = key.split("\\.")
      AlarmKey(keyParts(0), keyParts(1), keyParts(2))
    }
  }

  case class ComponentKey(subsystem: String, component: String) extends Key(subsystem, component, "*")
  case class SubsystemKey(subsystem: String)                    extends Key(subsystem, "*", "*")
  case object GlobalKey                                         extends Key("*", "*", "*")
}
