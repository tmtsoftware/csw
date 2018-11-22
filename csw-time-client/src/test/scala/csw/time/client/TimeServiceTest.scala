package csw.time.client

import java.time._

import csw.time.api.scaladsl.TimeService
import csw.time.client.internal.native_models.{NTPTimeVal, Timex}
import csw.time.client.internal.{TimeLibrary, TimeServiceImpl}
import csw.time.client.tags.Linux
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

class TimeServiceTest extends FunSuite with Matchers with BeforeAndAfterAll {
  val TaiOffset = 37

  override protected def beforeAll(): Unit = {
    val timex   = new Timex()
    val timeVal = new NTPTimeVal()

    // sets the tai offset on kernel (needed when ptp is not setup)
    timex.modes = 128
    timex.constant = TaiOffset
    TimeLibrary.ntp_adjtime(timex)
    println("Status of Tai offset command=" + timex.status)

    TimeLibrary.ntp_gettimex(timeVal)
    println(s"Tai offset set to [${timeVal.tai}]")
  }

  //DEOPSCSW-533: Access parts of UTC date.time in Java and Scala
  test("should get UTC time", Linux) {
    val timeService: TimeService = new TimeServiceImpl()

    val utcInstant            = timeService.utcTime()
    val fixedInstant: Instant = Instant.now()

    val expectedMillis = fixedInstant.toEpochMilli +- 5

    utcInstant.value.toEpochMilli shouldEqual expectedMillis
  }

  //DEOPSCSW-534: PTP accuracy and precision while reading UTC
  test("should get precision up to nanoseconds in UTC time", Linux) {
    val timeService: TimeService = new TimeServiceImpl()

    val utcInstant = timeService.utcTime()

    println(utcInstant.value)
    Utils.digits(utcInstant.value.getNano) shouldEqual 9
  }

  //DEOPSCSW-536: Access parts of TAI date/time in Java and Scala
  //DEOPSCSW-530: SPIKE: Get TAI offset and convert to UTC and Vice Versa
  test("should get TAI time", Linux) {
    val timeService: TimeService = new TimeServiceImpl()

    val taiOffset = 37

    val taiInstant          = timeService.taiTime()
    val TAIInstant: Instant = Instant.now().plusSeconds(taiOffset)

    val expectedMillis = TAIInstant.toEpochMilli +- 5

    taiInstant.value.toEpochMilli shouldEqual expectedMillis
  }

  //DEOPSCSW-530: SPIKE: Get TAI offset and convert to UTC and Vice Versa
  test("should get TAI offset", Linux) {
    val timeService: TimeService = new TimeServiceImpl()

    val offset = timeService.taiOffset()
    offset shouldEqual TaiOffset
  }
}
