package csw.time.client

import java.time._

import akka.actor.ActorSystem
import akka.testkit.TestProbe
import csw.time.api.models.CswInstant.{TaiInstant, UtcInstant}
import csw.time.api.scaladsl.TimeService
import csw.time.client.extensions.RichInstant.RichInstant
import csw.time.client.internal.TimeServiceImpl
import csw.time.client.tags.Linux
import org.scalatest.concurrent.Eventually
import org.scalatest.time.SpanSugar.convertDoubleToGrainOfTime
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

class TimeServiceTest extends FunSuite with Matchers with BeforeAndAfterAll with Eventually {
  val TaiOffset = 37

  override protected def beforeAll(): Unit = {
    val timeService = new TimeServiceImpl()
    timeService.setTaiOffset(TaiOffset)
  }

  //------------------------------UTC-------------------------------

  //DEOPSCSW-532: Synchronize activities with other comp. using UTC
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

    eventually(timeout = timeout(50.millis), interval = interval(10.millis))(
      timeService.utcTime().value.formatNanos should not endWith "000"
    )
  }

  //DEOPSCSW-533: Access parts of UTC date.time in Java and Scala
  test("should access parts of UTC time", Linux) {
    val utcInstant = UtcInstant(Instant.parse("2007-12-03T10:15:30.00Z"))

    val hstZone = ZoneId.of("-10:00")

    val hstZDT: ZonedDateTime = utcInstant.value.atZone(hstZone)

    hstZDT.getYear shouldBe 2007
    hstZDT.getMonth.getValue shouldBe 12
    hstZDT.getDayOfMonth shouldBe 3
    hstZDT.getHour shouldBe 0 // since HST is -10:00 from UTC
    hstZDT.getMinute shouldBe 15
    hstZDT.getSecond shouldBe 30
  }

  //------------------------------TAI-------------------------------

  //DEOPSCSW-535: Synchronize activities with other comp, using TAI
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

  //DEOPSCSW-538: PTP accuracy and precision while reading TAI
  test("should get precision up to nanoseconds in TAI time", Linux) {
    val timeService: TimeService = new TimeServiceImpl()

    eventually(timeout = timeout(50.millis), interval = interval(10.millis))(
      timeService.taiTime().value.formatNanos should not endWith "000"
    )
  }

  //DEOPSCSW-536: Access parts of TAI date.time in Java and Scala
  test("should access parts of TAI time", Linux) {
    val taiInstant = TaiInstant(Instant.parse("2007-12-03T10:15:30.00Z"))

    val hstZone = ZoneId.of("-10:00")

    val hstZDT = taiInstant.value.atZone(hstZone)

    hstZDT.getYear shouldBe 2007
    hstZDT.getMonth.getValue shouldBe 12
    hstZDT.getDayOfMonth shouldBe 3
    hstZDT.getHour shouldBe 0 // since HST is -10:00 from UTC
    hstZDT.getMinute shouldBe 15
    hstZDT.getSecond shouldBe 30
  }

  //DEOPSCSW-530: SPIKE: Get TAI offset and convert to UTC and Vice Versa
  test("should get TAI offset", Linux) {
    val timeService: TimeService = new TimeServiceImpl()

    val offset = timeService.taiOffset()
    offset shouldEqual TaiOffset
  }

  //------------------------------Scheduling-------------------------------

  //DEOPSCSW-542: Schedule a task to execute in future
  test("should schedule a task once at given start time with allowed jitter of 5ms", Linux) {
    val timeService: TimeService = new TimeServiceImpl()

    implicit val sys: ActorSystem = ActorSystem.create("time-service")
    val testProbe                 = TestProbe()
    val probeMsg                  = "Scheduled"

    var actualScheduleTime: TaiInstant = null
    val idealScheduleTime: TaiInstant  = TaiInstant(timeService.taiTime().value.plusSeconds(1))

    timeService.scheduleOnce(idealScheduleTime) {
      actualScheduleTime = timeService.taiTime()
      testProbe.ref ! probeMsg
    }

    testProbe.expectMsg(probeMsg)

    println(s"Ideal Schedule Time: $idealScheduleTime")
    println(s"Actual Schedule Time: $actualScheduleTime")

    val allowedJitterInNanos = 5 * 1000 * 1000 // should be ideally 400µs as per 3σ statistics from manual tests

    actualScheduleTime.value.getEpochSecond - idealScheduleTime.value.getEpochSecond shouldBe 0
    actualScheduleTime.value.getNano - idealScheduleTime.value.getNano should be < allowedJitterInNanos
  }
}
