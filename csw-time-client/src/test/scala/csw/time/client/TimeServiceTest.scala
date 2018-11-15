package csw.time.client

import java.time._

import csw.time.api.TimeService
import csw.time.client.tags.Linux
import org.scalatest.{FunSuite, Matchers}

class TimeServiceTest extends FunSuite with Matchers {

  private val fixedInstant: Instant = Instant.now()
  private val zoneId: ZoneId        = ZoneId.of("US/Hawaii")
  private val clock: Clock          = Clock.fixed(fixedInstant, zoneId)

  //DEOPSCSW-533: Access parts of UTC date.time in Java and Scala
  ignore("should get UTC time", Linux) {
    val timeService: TimeService = new TimeServiceImpl(clock)

    val instant               = timeService.UTCTime()
    val fixedInstant: Instant = Instant.now()

    val expectedMillis = fixedInstant.toEpochMilli +- 5

    instant.toEpochMilli shouldEqual expectedMillis
  }

  //DEOPSCSW-534: PTP accuracy and precision while reading UTC
  ignore("should get precision up to nanoseconds in UTC time", Linux) {
    val timeService: TimeService = new TimeServiceImpl(clock)

    val instant = timeService.UTCTime()

    println(instant)
    Utils.digits(instant.getNano) shouldEqual 9
  }

  //DEOPSCSW-536: Access parts of TAI date/time in Java and Scala
  ignore("should get TAI time", Linux) {
    val timeService: TimeService = new TimeServiceImpl(clock)

    val taiOffset = 37

    val instant               = timeService.TAITime()
    val fixedInstant: Instant = Instant.now().plusSeconds(taiOffset)

    val expectedMillis = fixedInstant.toEpochMilli +- 5

    instant.toEpochMilli shouldEqual expectedMillis
  }

  //DEOPSCSW-530: SPIKE: Get TAI offset and convert to UTC and Vice Versa
  ignore("should get TAI offset", Linux) {
    val timeService: TimeService = new TimeServiceImpl(clock)

    val expectedOffset = 37

    val offset = timeService.TAIOffset()

    offset shouldEqual expectedOffset
  }

}
