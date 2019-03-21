package csw.time.core.models

import java.time.{Duration, Instant}

import csw.time.clock.natives.models.TMTClock
import csw.time.core.tags.TimeTests
import org.scalatest.Matchers._
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import scala.concurrent.duration.DurationDouble

@TimeTests
class TMTTimeTest extends FunSuite with BeforeAndAfterAll {
  private val TaiOffset = 37

  override protected def beforeAll(): Unit = TMTClock.clock.setTaiOffset(TaiOffset)

  test("should get utc time") {
    val utcTime        = UTCTime.now()
    val fixedInstant   = Instant.now()
    val expectedMillis = fixedInstant.toEpochMilli

    utcTime.value.toEpochMilli.toDouble shouldEqual expectedMillis.toDouble +- 10
  }

  test("should convert utc to tai") {
    val utcTime = UTCTime.now()
    val taiTime = utcTime.toTAI
    Duration.between(utcTime.value, taiTime.value).getSeconds shouldEqual TaiOffset
  }

  test("should give time duration between given timestamp and current time") {
    val expectedDuration = 1.second.toMillis +- 10.millis.toMillis
    val futureTime       = UTCTime(Instant.now().plusSeconds(1))
    futureTime.durationFromNow.toMillis shouldBe expectedDuration
  }
}
