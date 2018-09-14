package csw.alarm.api.models
import java.time.{Clock, Instant}

/**
 * Represents time of severity change of an alarm
 */
case class AlarmTime private[alarm] (time: Instant) {

  /**
   * String representation of alarm time
   */
  val value: String = time.toString
}

object AlarmTime {

  private[alarm] def apply(): AlarmTime              = AlarmTime(Instant.now(Clock.systemUTC()))
  private[alarm] def apply(value: String): AlarmTime = AlarmTime(Instant.parse(value))
  private[alarm] def apply(time: Instant): AlarmTime = new AlarmTime(time)
}
