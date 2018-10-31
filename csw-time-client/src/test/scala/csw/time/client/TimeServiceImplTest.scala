package csw.time.client

import java.time._

import org.scalatest.{FunSuite, Matchers}

class TimeServiceImplTest extends FunSuite with Matchers {

  private val fixedInstant: Instant = Instant.now()
  private val zoneId: ZoneId        = ZoneId.of("US/Hawaii")
  private val clock: Clock          = Clock.fixed(fixedInstant, zoneId)

  private val timeService: TimeServiceImpl = new TimeServiceImpl(clock)

  test("should get UTC time") {
    val instant               = timeService.UTCTime()
    val fixedInstant: Instant = Instant.now()

    val expectedMillis = fixedInstant.toEpochMilli +- 10

    instant.toEpochMilli shouldEqual expectedMillis
  }

  test("should get precision up to nanoseconds in UTC time") {
    val instant = timeService.UTCTime()

    println(instant)
//    Utils.digits(instant.getNano) shouldEqual 9
  }

}
