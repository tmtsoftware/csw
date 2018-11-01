package csw.time.client

import java.time._

import csw.time.api.TimeService
import csw.time.client.tags.NanoPrecisionTag
import org.scalatest.{FunSuite, Matchers}

class TimeServiceTest extends FunSuite with Matchers {

  private val fixedInstant: Instant = Instant.now()
  private val zoneId: ZoneId        = ZoneId.of("US/Hawaii")
  private val clock: Clock          = Clock.fixed(fixedInstant, zoneId)

  private val timeService: TimeService = new TimeServiceImpl(clock)

  //DEOPSCSW-533: Access parts of UTC date.time in Java and Scala
  test("should get UTC time") {
    val instant               = timeService.UTCTime()
    val fixedInstant: Instant = Instant.now()

    val expectedMillis = fixedInstant.toEpochMilli +- 10

    instant.toEpochMilli shouldEqual expectedMillis
  }

  //DEOPSCSW-533: Access parts of UTC date.time in Java and Scala
  test("should get precision up to nanoseconds in UTC time", NanoPrecisionTag) {
    val instant = timeService.UTCTime()

    println(instant)
    Utils.digits(instant.getNano) shouldEqual 9
  }

}
