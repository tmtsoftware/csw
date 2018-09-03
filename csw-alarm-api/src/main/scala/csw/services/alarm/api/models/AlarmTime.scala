package csw.services.alarm.api.models
import java.time.{Clock, Instant}

/**
 * Basic model to represent time in the alarm service.
 *
 * @param time to represent in model.
 */
case class AlarmTime private[alarm] (time: Instant) {
  val value: String = time.toString
}

object AlarmTime {

  private[alarm] def apply(): AlarmTime              = AlarmTime(Instant.now(Clock.systemUTC()))
  private[alarm] def apply(value: String): AlarmTime = AlarmTime(Instant.parse(value))
  private[alarm] def apply(time: Instant): AlarmTime = new AlarmTime(time)
}
