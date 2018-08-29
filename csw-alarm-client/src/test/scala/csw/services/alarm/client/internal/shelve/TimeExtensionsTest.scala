package csw.services.alarm.client.internal.shelve

import java.time._

import csw.services.alarm.client.internal.extensions.TimeExtensions.RichClock
import csw.services.alarm.client.internal.shelve.TestTimeExtensions.TestClock
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.duration.DurationDouble

// DEOPSCSW-449: Set Shelve/Unshelve status for alarm entity
class TimeExtensionsTest extends FunSuite with Matchers {

  private val EpochClock: Clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)

  test("untilNext text") {
    EpochClock.untilNext("10:30:30 AM").getSeconds shouldBe 10.hours.plus(30.minutes).plus(30.seconds).toSeconds
    EpochClock.plusHours(11).untilNext("10:30:30 AM").getSeconds shouldBe 23.hours.plus(30.minutes).plus(30.seconds).toSeconds

    EpochClock.untilNext("10:30:30 PM").getSeconds shouldBe 22.hours.plus(30.minutes).plus(30.seconds).toSeconds
    EpochClock.plusHours(11).untilNext("10:30:30 PM").getSeconds shouldBe 11.hours.plus(30.minutes).plus(30.seconds).toSeconds
  }

}
