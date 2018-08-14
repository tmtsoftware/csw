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
sealed abstract class Key(subsystem: String, component: String, name: String) {
  require(!isNullOrEmpty(subsystem), "subsystem should not be an empty value")
  require(!isNullOrEmpty(component), "component should not be an empty value")
  require(!isNullOrEmpty(name), "name should not be an empty value")
  def value: String = s"${subsystem.toLowerCase}.${component.toLowerCase}.${name.toLowerCase}"

  private def isNullOrEmpty(value: String) = value == null || value.isEmpty
}

object Key {
  // pattern matches for any one of *, [, ], ^, - characters  present
  val patternForInvalidKey: Pattern = Pattern.compile(".*[\\*\\[\\]\\^\\?\\-].*")

  case class AlarmKey(subsystem: String, component: String, name: String) extends Key(subsystem, component, name) {
    require(!patternForInvalidKey.matcher(value).matches(), "key contains invalid characters")
  }
  case class ComponentKey(subsystem: String, component: String) extends Key(subsystem, component, "*")
  case class SubsystemKey(subsystem: String)                    extends Key(subsystem, "*", "*")
  case object GlobalKey                                         extends Key("*", "*", "*")
}
