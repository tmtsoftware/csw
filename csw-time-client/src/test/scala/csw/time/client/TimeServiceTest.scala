package csw.time.client

import java.time._

import csw.time.api.scaladsl.TimeService
import csw.time.client.internal.TimeServiceImpl
import csw.time.client.tags.Linux
import org.scalatest.{FunSuite, Matchers}

class TimeServiceTest extends FunSuite with Matchers {

  //DEOPSCSW-533: Access parts of UTC date.time in Java and Scala
  test("should get UTC time", Linux) {
    val timeService: TimeService = new TimeServiceImpl()

    val utcInstant            = timeService.UtcTime()
    val fixedInstant: Instant = Instant.now()

    val expectedMillis = fixedInstant.toEpochMilli +- 5

    utcInstant.value.toEpochMilli shouldEqual expectedMillis
  }

  //DEOPSCSW-534: PTP accuracy and precision while reading UTC
  test("should get precision up to nanoseconds in UTC time", Linux) {
    val timeService: TimeService = new TimeServiceImpl()

    val utcInstant = timeService.UtcTime()

    println(utcInstant.value)
    Utils.digits(utcInstant.value.getNano) shouldEqual 9
  }

  //DEOPSCSW-536: Access parts of TAI date/time in Java and Scala
  //DEOPSCSW-530: SPIKE: Get TAI offset and convert to UTC and Vice Versa
  test("should get TAI time", Linux) {
    val timeService: TimeService = new TimeServiceImpl()

    val taiOffset = 37

    val taiInstant          = timeService.TaiTime()
    val TAIInstant: Instant = Instant.now().plusSeconds(taiOffset)

    val expectedMillis = TAIInstant.toEpochMilli +- 5

    taiInstant.value.toEpochMilli shouldEqual expectedMillis
  }

  //DEOPSCSW-530: SPIKE: Get TAI offset and convert to UTC and Vice Versa
  test("should get TAI offset", Linux) {
    val timeService: TimeService = new TimeServiceImpl()

    val expectedOffset = 37

    val offset = timeService.TaiOffset()

    offset shouldEqual expectedOffset
  }
}
