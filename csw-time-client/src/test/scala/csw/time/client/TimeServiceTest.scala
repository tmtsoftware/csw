package csw.time.client

import java.time._

import csw.time.api.{TimeScales, TimeService}
import csw.time.client.tags.Linux
import org.scalatest.{FunSuite, Matchers}

class TimeServiceTest extends FunSuite with Matchers {

  private val fixedInstant: Instant = Instant.now()
  private val zoneId: ZoneId        = ZoneId.of("US/Hawaii")
  private val clock: Clock          = Clock.fixed(fixedInstant, zoneId)

  //DEOPSCSW-533: Access parts of UTC date.time in Java and Scala
  test("should get UTC time", Linux) {
    val timeService: TimeService = new TimeServiceImpl(clock)

    val cswInstant            = timeService.UTCTime()
    val fixedInstant: Instant = Instant.now()

    val expectedMillis = fixedInstant.toEpochMilli +- 5

    cswInstant.instant.toEpochMilli shouldEqual expectedMillis
    cswInstant.timeScale shouldBe TimeScales.UTCScale
  }

  //DEOPSCSW-534: PTP accuracy and precision while reading UTC
  test("should get precision up to nanoseconds in UTC time", Linux) {
    val timeService: TimeService = new TimeServiceImpl(clock)

    val cswInstant = timeService.UTCTime()

    println(cswInstant.instant)
    Utils.digits(cswInstant.instant.getNano) shouldEqual 9
    cswInstant.timeScale shouldBe TimeScales.UTCScale
  }

  //DEOPSCSW-536: Access parts of TAI date/time in Java and Scala
  test("should get TAI time", Linux) {
    val timeService: TimeService = new TimeServiceImpl(clock)

    val taiOffset = 37

    val cswInstant            = timeService.TAITime()
    val fixedInstant: Instant = Instant.now().plusSeconds(taiOffset)

    val expectedMillis = fixedInstant.toEpochMilli +- 5

    cswInstant.instant.toEpochMilli shouldEqual expectedMillis
    cswInstant.timeScale shouldBe TimeScales.TAIScale
  }

  //DEOPSCSW-530: SPIKE: Get TAI offset and convert to UTC and Vice Versa
  test("should get TAI offset", Linux) {
    val timeService: TimeService = new TimeServiceImpl(clock)

    val expectedOffset = 37

    val offset = timeService.TAIOffset()

    offset shouldEqual expectedOffset
  }

}
