package csw.services.alarm.client.internal.shelve

import java.time.{Clock, Instant, ZoneOffset}

import csw.services.alarm.client.internal.extensions.TimeExtensions.RichClock
import csw.services.alarm.client.internal.shelve.TestTimeExtensions.{TestClock, TestInt}
import org.scalatest.{FunSuite, Matchers}

class TimeExtensionsTest extends FunSuite with Matchers {

  private val EpochClock: Clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)

  test("untilNext hour") {
    EpochClock.untilNext(10) shouldBe 10.hours
    EpochClock.plusDays(123).untilNext(10) shouldBe 10.hours

    EpochClock.untilNext(20) shouldBe 20.hours
    EpochClock.plusDays(123).untilNext(20) shouldBe 20.hours

    EpochClock.plusHours(20).untilNext(10) shouldBe 14.hours
    EpochClock.plusDays(123).plusHours(20).untilNext(10) shouldBe 14.hours
  }

  test("untilNext text") {
    EpochClock.untilNext("10:30 AM") shouldBe 10.hours.plus(30.minutes)
    EpochClock.plusHours(11).untilNext("10:30 AM") shouldBe 23.hours.plus(30.minutes)

    EpochClock.untilNext("10:30 PM") shouldBe 22.hours.plus(30.minutes)
    EpochClock.plusHours(11).untilNext("10:30 PM") shouldBe 11.hours.plus(30.minutes)
  }

}
