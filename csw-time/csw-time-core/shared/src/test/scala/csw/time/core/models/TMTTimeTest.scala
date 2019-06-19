package csw.time.core.models

import java.time.{Duration, Instant}

import csw.time.clock.natives.models.TimeConstants
import org.scalatest.FunSuite
import org.scalatest.Matchers._

import scala.concurrent.duration.DurationDouble

class TMTTimeTest extends FunSuite {
  private val jitter = 10

  test("should get utc time") {
    val utcTime        = UTCTime.now()
    val fixedInstant   = Instant.now()
    val expectedMillis = fixedInstant.toEpochMilli

    utcTime.value.toEpochMilli.toDouble shouldEqual expectedMillis.toDouble +- jitter
  }

  test("should convert utc to tai") {
    val utcTime = UTCTime.now()
    val taiTime = utcTime.toTAI
    Duration.between(utcTime.value, taiTime.value).getSeconds shouldEqual TimeConstants.taiOffset
  }

  test("should give time duration between given timestamp and current time") {
    val expectedDuration = 1.second.toMillis +- jitter.millis.toMillis
    val futureTime       = UTCTime(Instant.now().plusSeconds(1))
    futureTime.durationFromNow.toMillis shouldBe expectedDuration
  }
}
