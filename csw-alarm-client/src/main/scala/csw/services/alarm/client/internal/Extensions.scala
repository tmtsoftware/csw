package csw.services.alarm.client.internal

import java.time.temporal.ChronoUnit
import java.time.{ZoneOffset, ZonedDateTime}

import scala.compat.java8.DurationConverters.DurationOps
import scala.concurrent.duration.FiniteDuration

object Extensions {

  implicit class RichZonedDateTime(value: ZonedDateTime) {
    def -(targetDate: ZonedDateTime): FiniteDuration = {
      val duration = java.time.Duration.between(targetDate, value)
      duration.toScala
    }
  }

  implicit class RichInt(value: Int) {

    val TWENTY_HOUR_FORMAT_MAX_LIMIT = 23
    val TWENTY_HOUR_FORMAT_MIN_LIMIT = 0
    val TWELVE_FORMAT_MAX_LIMIT      = 12
    val TWELVE_FORMAT_MIN_LIMIT      = 1

    /**
     * Converts an integer to ZonedDateTime with UTC offset
     * @return next instant of time with hour set to given value
     */
    def toHourOfDay: ZonedDateTime = {
      hourOfDay(TWENTY_HOUR_FORMAT_MIN_LIMIT, TWENTY_HOUR_FORMAT_MAX_LIMIT)
    }

    def am: ZonedDateTime = {
      hourOfDay(TWELVE_FORMAT_MIN_LIMIT, TWELVE_FORMAT_MAX_LIMIT)
    }

    def pm: ZonedDateTime = {
      hourOfDay(TWELVE_FORMAT_MIN_LIMIT, TWELVE_FORMAT_MAX_LIMIT).plusHours(12)
    }

    private def hourOfDay(minHour: Int, maxHour: Int) = {
      if (value < minHour || value > maxHour)
        throw new IllegalArgumentException(s"time should be between 1 & $maxHour")

      val currentDate = ZonedDateTime.now(ZoneOffset.UTC)

      val date = currentDate
        .truncatedTo(ChronoUnit.DAYS)
        .withHour(value)

      if (date.compareTo(currentDate) > 0)
        date
      else date.plusDays(1)
    }
  }
}
