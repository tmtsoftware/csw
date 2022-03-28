/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.alarm.client.internal.extensions

import java.time._
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}

private[alarm] object TimeExtensions {

  val TimeFormatter: DateTimeFormatter =
    new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("h:m:s a").toFormatter

  implicit class RichClock(val clock: Clock) extends AnyVal {
    def untilNext(text: String): Duration = untilNext(LocalTime.parse(text, TimeFormatter))

    private def untilNext(localTime: LocalTime): Duration = {
      val currentTime = LocalTime.now(clock)
      val targetTime  = localTime.adjustInto(currentTime) // Adjust localTime to the timezone of clock. In our case UTC timezone.
      val duration    = Duration.between(currentTime, targetTime)
      if (duration.isNegative) duration.plusDays(1) else duration
    }
  }
}
