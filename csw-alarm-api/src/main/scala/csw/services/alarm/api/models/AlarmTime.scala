package csw.services.alarm.api.models
import java.time.{Clock, Instant}

case class AlarmTime(time: Instant) {
  val value: String = time.toString
}

object AlarmTime {
  def apply(): AlarmTime              = AlarmTime(Instant.now(Clock.systemUTC()))
  def apply(value: String): AlarmTime = AlarmTime(Instant.parse(value))
}
