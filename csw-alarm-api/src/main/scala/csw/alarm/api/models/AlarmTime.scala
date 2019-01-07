package csw.alarm.api.models
import java.time.Instant

import csw.time.api.models.UTCTime

/**
 * Represents time of severity change of an alarm
 */
case class AlarmTime private[alarm] (time: UTCTime) {

  /**
   * String representation of alarm time
   */
  val value: String = time.value.toString
}

object AlarmTime {

  private[alarm] def apply(): AlarmTime              = AlarmTime(UTCTime.now())
  private[alarm] def apply(value: String): AlarmTime = AlarmTime(UTCTime(Instant.parse(value)))
  private[alarm] def apply(time: UTCTime): AlarmTime = new AlarmTime(time)
}
