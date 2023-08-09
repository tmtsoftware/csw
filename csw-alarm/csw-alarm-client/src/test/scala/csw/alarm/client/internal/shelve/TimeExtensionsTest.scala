/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.alarm.client.internal.shelve

import java.time.*

import csw.alarm.client.internal.extensions.TimeExtensions.RichClock
import csw.alarm.client.internal.shelve.TestTimeExtensions.TestClock

import scala.concurrent.duration.DurationDouble
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

// DEOPSCSW-449: Set Shelve/Unshelve status for alarm entity
class TimeExtensionsTest extends AnyFunSuite with Matchers {

  private val EpochClock: Clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)

  test("untilNext text | DEOPSCSW-449") {
    EpochClock.untilNext("10:30:30 AM").getSeconds shouldBe 10.hours.plus(30.minutes).plus(30.seconds).toSeconds
    EpochClock.plusHours(11).untilNext("10:30:30 AM").getSeconds shouldBe 23.hours.plus(30.minutes).plus(30.seconds).toSeconds

    EpochClock.untilNext("10:30:30 PM").getSeconds shouldBe 22.hours.plus(30.minutes).plus(30.seconds).toSeconds
    EpochClock.plusHours(11).untilNext("10:30:30 PM").getSeconds shouldBe 11.hours.plus(30.minutes).plus(30.seconds).toSeconds
  }

}
