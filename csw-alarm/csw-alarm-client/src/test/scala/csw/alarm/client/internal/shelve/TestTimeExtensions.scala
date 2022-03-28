/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.alarm.client.internal.shelve
import java.time.{Clock, Duration}

object TestTimeExtensions {
  implicit class TestClock(clock: Clock) {
    def plusHours(hours: Int): Clock = Clock.offset(clock, Duration.ofHours(hours))
    def plusDays(days: Int): Clock   = Clock.offset(clock, Duration.ofDays(days))
  }

  implicit class TestInt(value: Int) {
    def hours: Duration   = Duration.ofHours(value)
    def minutes: Duration = Duration.ofMinutes(value)
  }
}
