/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.time.core.models

import java.time.{Duration, Instant}

import csw.time.clock.natives.models.TimeConstants
import org.scalatest.matchers
import matchers.should.Matchers._

import scala.concurrent.duration.DurationDouble
import scala.concurrent.duration.DurationInt
import org.scalatest.funsuite.AnyFunSuite

// DEOPSCSW-549: Time service api
class TMTTimeTest extends AnyFunSuite {
  private val jitter = 100

  test("should get utc time | DEOPSCSW-549") {
    val utcTime        = UTCTime.now()
    val fixedInstant   = Instant.now()
    val expectedMillis = fixedInstant.toEpochMilli

    utcTime.value.toEpochMilli.toDouble shouldEqual expectedMillis.toDouble +- jitter
  }

  test("should convert utc to tai | DEOPSCSW-549") {
    val utcTime = UTCTime.now()
    val taiTime = utcTime.toTAI
    Duration.between(utcTime.value, taiTime.value).getSeconds shouldEqual TimeConstants.taiOffset
  }

  test("should give time duration between given timestamp and current time | DEOPSCSW-549") {
    val expectedDuration = 1.second.toMillis +- jitter.millis.toMillis
    val futureTime       = UTCTime(Instant.now().plusSeconds(1))
    futureTime.durationFromNow.toMillis shouldBe expectedDuration
  }

  test("should give utc time after specified duration | DEOPSCSW-549") {
    val tenSeconds = 10.seconds
    val futureTime = UTCTime.after(tenSeconds)

    futureTime.durationFromNow.toMillis shouldBe (tenSeconds.toMillis +- jitter.millis.toMillis)
  }

  test("should give tai time after specified duration | DEOPSCSW-549") {
    val tenSeconds = 10.seconds
    val futureTime = TAITime.after(tenSeconds)

    futureTime.durationFromNow.toMillis shouldBe (tenSeconds.toMillis +- jitter.millis.toMillis)
  }
}
