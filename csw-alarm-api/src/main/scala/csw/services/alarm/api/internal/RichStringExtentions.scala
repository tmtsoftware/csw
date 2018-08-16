package csw.services.alarm.api.internal
import java.util.regex.Pattern

private[alarm] object RichStringExtentions {
  implicit class RichString(val value: String) extends AnyVal {
    def matches(pattern: Pattern): Boolean = pattern.matcher(value).matches()
    def isDefined: Boolean                 = value != null && !value.isEmpty
  }
}
